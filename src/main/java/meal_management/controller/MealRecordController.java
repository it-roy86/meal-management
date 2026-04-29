package meal_management.controller;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import meal_management.entity.MealRecord;
import meal_management.service.MealRecordService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

/**
 * 식사 기록 관련 API를 처리하는 컨트롤러예요.
 * 일일 식사 인원 입력, 날짜별 조회 기능을 담당해요.
 */
@RestController
@RequestMapping("/api/meal-records")
@RequiredArgsConstructor
public class MealRecordController {

    private final MealRecordService mealRecordService;

    /**
     * 식사 기록 요청 DTO
     * Vue.js에서 보내는 JSON 데이터를 받아요.
     */
    @Getter
    @Setter
    public static class MealRecordRequest {
        private String recordDate;     // 날짜 (yyyy-MM-dd)
        private Long companyId;        // 회사 ID
        private Long companyTeamId;    // 팀 ID
        private Integer lunchCount;    // 중식 인원
        private Integer dinnerCount;   // 석식 인원
    }

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

    /**
     * 식사 기록 저장 API
     * POST /api/meal-records
     *
     * 요청 예시:
     * {
     *   "recordDate": "2026-04-25",
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

    @GetMapping
    public ResponseEntity<List<MealRecordResponse>> getMealRecords(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long companyId) {

        List<MealRecord> records;

        if (companyId != null) {
            records = mealRecordService.getMealRecordsByCompanyAndDateRange(companyId, startDate, endDate);
        } else {
            records = mealRecordService.getMealRecordsByDateRange(startDate, endDate);
        }

        // MealRecord → MealRecordResponse 변환
        List<MealRecordResponse> response = records.stream()
                .map(MealRecordResponse::new)
                .collect(java.util.stream.Collectors.toList());

        return ResponseEntity.ok(response);
    }
}