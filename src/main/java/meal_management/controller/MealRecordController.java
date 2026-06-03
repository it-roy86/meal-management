package meal_management.controller;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import meal_management.entity.MealRecord;
import meal_management.service.MealRecordService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 식사 기록 관련 API를 처리하는 컨트롤러예요.
 * 일일 식사 인원 입력, 날짜별 조회 기능을 담당해요.
 * VIEWER는 자기 회사 데이터만 조회할 수 있어요.
 */
@RestController
@RequestMapping("/api/meal-records")
@RequiredArgsConstructor
public class MealRecordController {

    private final MealRecordService mealRecordService;

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

        // JWT에서 현재 로그인한 사용자 역할과 회사 ID 가져오기
        String role = getCurrentRole();
        Long viewerCompanyId = getCurrentCompanyId();

        List<MealRecord> records;

        // VIEWER면 자기 회사 데이터만 강제 조회 (파라미터 무시)
        if ("VIEWER".equals(role)) {
            if (viewerCompanyId == null) {
                return ResponseEntity.ok(Collections.emptyList());
            }
            records = mealRecordService.getMealRecordsByCompanyAndDateRange(
                    viewerCompanyId, startDate, endDate);

        } else if (companyId != null) {
            // ADMIN이 특정 회사 선택한 경우
            records = mealRecordService.getMealRecordsByCompanyAndDateRange(
                    companyId, startDate, endDate);

        } else {
            // ADMIN 전체 조회
            records = mealRecordService.getMealRecordsByDateRange(startDate, endDate);
        }

        // MealRecord → MealRecordResponse 변환
        List<MealRecordResponse> response = records.stream()
                .map(MealRecordResponse::new)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    // ========================
    // 내부 유틸 메서드
    // ========================

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