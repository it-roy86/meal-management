package meal_management.controller;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import meal_management.entity.MealRecord;
import meal_management.service.MealRecordExcelService;
import meal_management.service.MealRecordService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 식사 기록 관련 API를 처리하는 컨트롤러예요.
 * 일일 식사 인원 입력, 날짜별 조회, 조회 결과 엑셀 다운로드 기능을 담당해요.
 * VIEWER는 자기 회사 데이터만 조회할 수 있어요.
 */
@Slf4j
@RestController
@RequestMapping("/api/meal-records")
@RequiredArgsConstructor
public class MealRecordController {

    // 엑셀(.xlsx) 파일의 Content-Type
    private static final MediaType XLSX_MEDIA_TYPE = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final MealRecordService mealRecordService;
    private final MealRecordExcelService mealRecordExcelService;

    // ========================
    // 요청 DTO
    // ========================

    /**
     * 식사 기록 요청 DTO
     * Vue.js에서 보내는 JSON 데이터를 받아요.
     */
    @Getter
    @Setter
    public static class MealRecordRequest {
        private String recordDate;      // 날짜 (yyyy-MM-dd)
        private Long companyId;         // 회사 ID
        private Long companyTeamId;     // 팀 ID
        private Integer lunchCount;     // 중식 인원
        private Integer dinnerCount;    // 석식 인원
    }

    /**
     * 식사 기록 수정 요청 DTO
     * 중식/석식 인원만 수정 가능해요.
     */
    @Getter
    @Setter
    public static class MealRecordUpdateRequest {
        private Integer lunchCount;     // 수정할 중식 인원
        private Integer dinnerCount;    // 수정할 석식 인원
    }

    // ========================
    // 응답 DTO
    // ========================

    /**
     * 식사 기록 응답 DTO
     * @JsonIgnore로 숨겨진 회사명/팀명을 포함해서 Vue.js에 전달해요.
     */
    @Getter
    public static class MealRecordResponse {
        private Long id;
        private String recordDate;
        private String companyName;
        private String teamName;
        private Integer lunchCount;
        private Integer dinnerCount;
        private Integer totalCount;
        private Integer totalAmount;

        public MealRecordResponse(MealRecord record) {
            this.id = record.getId();
            this.recordDate = record.getRecordDate().toString();
            this.companyName = record.getCompany().getCompanyName();
            this.teamName = record.getCompanyTeam().getTeamName();
            this.lunchCount = record.getLunchCount();
            this.dinnerCount = record.getDinnerCount();
            this.totalCount = record.getTotalCount();
            this.totalAmount = record.getTotalAmount();
        }
    }

    // ========================
    // API
    // ========================

    /**
     * 식사 기록 저장 API
     * POST /api/meal-records
     *
     * 요청 예시:
     * {
     *   "recordDate": "2026-04-29",
     *   "companyId": 1,
     *   "companyTeamId": 1,
     *   "lunchCount": 10,
     *   "dinnerCount": 5
     * }
     */
    @PostMapping
    public ResponseEntity<MealRecord> createMealRecord(
            @RequestBody MealRecordRequest request) {

        // String 날짜를 LocalDate로 변환
        LocalDate recordDate = LocalDate.parse(request.getRecordDate());

        MealRecord mealRecord = mealRecordService.createMealRecord(
                recordDate,
                request.getCompanyId(),
                request.getCompanyTeamId(),
                request.getLunchCount(),
                request.getDinnerCount()
        );

        return ResponseEntity.ok(mealRecord);
    }

    /**
     * 날짜 범위 + 회사별 식사 기록 조회 API
     * GET /api/meal-records?startDate=2026-04-01&endDate=2026-04-30&companyId=1(선택)
     *
     * ADMIN: companyId 파라미터로 전체 또는 회사별 조회
     * VIEWER: JWT의 companyId로 자기 회사만 강제 조회
     */
    @GetMapping
    public ResponseEntity<List<MealRecordResponse>> getMealRecords(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long companyId) {

        String role = getCurrentRole();
        log.info("식사 기록 조회 요청: role={}, startDate={}, endDate={}, companyId={}",
                role, startDate, endDate, companyId);

        try {
            // 역할별 조회 범위 적용 (VIEWER는 자기 회사만)
            List<MealRecord> records = findRecordsForCurrentUser(startDate, endDate, companyId);

            // MealRecord → MealRecordResponse 변환
            List<MealRecordResponse> response = records.stream()
                    .map(MealRecordResponse::new)
                    .collect(Collectors.toList());

            log.info("식사 기록 조회 완료: role={}, startDate={}, endDate={}, 결과={}건",
                    role, startDate, endDate, response.size());
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            // 요청/결과 로그를 한 쌍으로 남기기 위해 실패도 여기서 기록해요.
            // 예외를 다시 던지면 전역 예외 처리기가 없어서 Tomcat이 같은 스택트레이스를 한 번 더 찍기 때문에,
            // 여기서 500 응답을 직접 돌려줘요 (프론트는 어느 쪽이든 실패 alert만 띄워요).
            log.error("식사 기록 조회 실패: role={}, startDate={}, endDate={}, companyId={}",
                    role, startDate, endDate, companyId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 식사 기록 엑셀 다운로드 API
     * GET /api/meal-records/excel?startDate=2026-09-01&endDate=2026-09-30&companyId=1(선택)
     *
     * 조회 조건과 권한은 위의 조회 API(GET /api/meal-records)와 완전히 같아요.
     * ADMIN: companyId 파라미터로 전체 또는 회사별
     * VIEWER: JWT의 companyId로 자기 회사만 강제 (파라미터 무시)
     *
     * 응답: .xlsx 파일 (날짜 오름차순 + 맨 아래 합계 행, MealRecordExcelService 참고)
     * 응답 헤더 예시:
     *   Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
     *   Content-Disposition: attachment; filename*=UTF-8''%EC%8B%9D%EB%8C%80%EB%82%B4%EC%97%AD_...xlsx
     *
     * 파일명 예시:
     *   회사 지정(ADMIN 회사 선택 / VIEWER): 식대내역_대박유통_2026-09-01_2026-09-30.xlsx
     *   전체 조회:                          식대내역_2026-09-01_2026-09-30.xlsx
     */
    @GetMapping("/excel")
    public ResponseEntity<byte[]> downloadMealRecordsExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long companyId) {

        String role = getCurrentRole();
        log.info("식사 기록 엑셀 다운로드 요청: role={}, startDate={}, endDate={}, companyId={}",
                role, startDate, endDate, companyId);

        try {
            List<MealRecord> records = findRecordsForCurrentUser(startDate, endDate, companyId);
            byte[] excel = mealRecordExcelService.createMealRecordExcel(records);

            // 회사가 지정된 조회(VIEWER 또는 ADMIN의 회사 선택)면 파일명에 회사명을 넣어요.
            // 회사명은 조회된 기록에서 꺼내요 (결과가 0건이면 회사명 없이 기간만).
            boolean companyFiltered = "VIEWER".equals(role) || companyId != null;
            String companyName = companyFiltered && !records.isEmpty()
                    ? records.get(0).getCompany().getCompanyName()
                    : null;
            String filename = buildExcelFilename(companyName, startDate, endDate);

            log.info("식사 기록 엑셀 다운로드 완료: role={}, 파일명={}, 결과={}건, 크기={}bytes",
                    role, filename, records.size(), excel.length);

            // 한글 파일명은 filename*=UTF-8''... 형식으로 인코딩돼요 (프론트 src/utils/download.js의 parseFilename이 읽음)
            ContentDisposition disposition = ContentDisposition.attachment()
                    .filename(filename, StandardCharsets.UTF_8)
                    .build();

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                    .contentType(XLSX_MEDIA_TYPE)
                    .body(excel);

        } catch (RuntimeException e) {
            // 요청/결과 로그를 한 쌍으로 남기기 위해 실패도 여기서 기록하고 500을 직접 돌려줘요 (조회 API와 같은 이유).
            // 엑셀 생성 실패의 스택트레이스도 여기서 한 번만 남아요 — MealRecordExcelService는 로그 없이 예외만 던짐.
            log.error("식사 기록 엑셀 다운로드 실패: role={}, startDate={}, endDate={}, companyId={}",
                    role, startDate, endDate, companyId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ========================
    // 내부 유틸 메서드
    // ========================

    /**
     * 현재 로그인한 사용자의 역할에 맞춰 식사 기록 조회
     * 조회 API와 엑셀 다운로드 API가 같이 써요 — 권한 분기가 두 곳에서 달라지면
     * VIEWER가 엑셀로 다른 회사 데이터를 받아가는 구멍이 생길 수 있어서 한 곳에 모아뒀어요.
     *
     * VIEWER: JWT의 companyId로 자기 회사만 강제 조회 (companyId 파라미터 무시)
     * ADMIN/OPERATOR: companyId가 있으면 그 회사만, 없으면 전체
     */
    private List<MealRecord> findRecordsForCurrentUser(
            LocalDate startDate, LocalDate endDate, Long companyId) {

        // JWT에서 현재 로그인한 사용자 역할 가져오기
        String role = getCurrentRole();

        // VIEWER면 자기 회사 데이터만 강제 조회 (파라미터 무시)
        if ("VIEWER".equals(role)) {
            Long viewerCompanyId = getCurrentCompanyId();
            if (viewerCompanyId == null) {
                log.debug("VIEWER인데 회사 ID가 없어서 빈 결과 반환");
                return Collections.emptyList();
            }
            log.debug("VIEWER 자기 회사 조회: companyId={}", viewerCompanyId);
            return mealRecordService.getMealRecordsByCompanyAndDateRange(
                    viewerCompanyId, startDate, endDate);
        }

        if (companyId != null) {
            // ADMIN이 특정 회사 선택한 경우
            return mealRecordService.getMealRecordsByCompanyAndDateRange(
                    companyId, startDate, endDate);
        }

        // ADMIN 전체 조회
        return mealRecordService.getMealRecordsByDateRange(startDate, endDate);
    }

    /**
     * 엑셀 파일명 생성
     * 예) 식대내역_대박유통_2026-09-01_2026-09-30.xlsx / 식대내역_2026-09-01_2026-09-30.xlsx
     * 회사명에 파일명으로 못 쓰는 문자(\ / : * ? " < > |)가 있으면 _로 바꿔요.
     */
    private String buildExcelFilename(String companyName, LocalDate startDate, LocalDate endDate) {
        StringBuilder name = new StringBuilder("식대내역");
        if (companyName != null && !companyName.isBlank()) {
            name.append('_').append(companyName.replaceAll("[\\\\/:*?\"<>|]", "_"));
        }
        name.append('_').append(startDate).append('_').append(endDate).append(".xlsx");
        return name.toString();
    }

    /**
     * 현재 로그인한 사용자의 역할 조회
     * JWT에서 바로 꺼내와요. DB 조회 없이 처리 가능해요!
     */
    private String getCurrentRole() {
        return SecurityContextHolder.getContext()
                .getAuthentication()
                .getAuthorities()
                .iterator()
                .next()
                .getAuthority()
                .replace("ROLE_", "");
    }

    /**
     * 현재 로그인한 VIEWER의 회사 ID 조회
     * JwtAuthenticationFilter에서 credentials에 저장한 companyId를 꺼내요.
     * ADMIN/OPERATOR는 null 반환해요.
     */
    private Long getCurrentCompanyId() {
        Object credentials = SecurityContextHolder.getContext()
                .getAuthentication()
                .getCredentials();
        return credentials instanceof Long ? (Long) credentials : null;
    }

    /**
     * 식사 기록 수정 API
     * PUT /api/meal-records/{id}
     *
     * 요청 예시:
     * {
     *   "lunchCount": 15,
     *   "dinnerCount": 8
     * }
     */
    @PutMapping("/{id}")
    public ResponseEntity<MealRecordResponse> updateMealRecord(
            @PathVariable Long id,
            @RequestBody MealRecordUpdateRequest request) {

        MealRecord mealRecord = mealRecordService.updateMealRecord(
                id,
                request.getLunchCount(),
                request.getDinnerCount()
        );

        return ResponseEntity.ok(new MealRecordResponse(mealRecord));
    }
}