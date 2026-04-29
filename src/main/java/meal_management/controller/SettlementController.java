package meal_management.controller;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import meal_management.entity.MealRecord;
import meal_management.service.MealRecordService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 월별 정산 API를 처리하는 컨트롤러예요.
 * 선택한 년월의 식사 기록을 회사/팀별로 집계해서 반환해요.
 */
@RestController
@RequestMapping("/api/settlement")
@RequiredArgsConstructor
public class SettlementController {

    private final MealRecordService mealRecordService;

    // ========================
    // 응답 DTO
    // ========================

    /**
     * 팀별 정산 데이터
     */
    @Getter
    public static class TeamSettlement {
        private String teamName;
        private int lunchCount;      // 중식 총 인원
        private int dinnerCount;     // 석식 총 인원
        private int lunchAmount;     // 중식 총 금액
        private int dinnerAmount;    // 석식 총 금액
        private int totalAmount;     // 합계 금액

        public TeamSettlement(String teamName, List<MealRecord> records) {
            this.teamName = teamName;

            // 팀의 중식/석식 인원 합계
            this.lunchCount = records.stream()
                    .mapToInt(r -> r.getLunchCount() == null ? 0 : r.getLunchCount())
                    .sum();
            this.dinnerCount = records.stream()
                    .mapToInt(r -> r.getDinnerCount() == null ? 0 : r.getDinnerCount())
                    .sum();

            // 금액 계산 (단가는 팀에서 가져옴)
            int lunchPrice = records.isEmpty() ? 0
                    : (records.get(0).getCompanyTeam().getLunchPrice() == null ? 0
                    : records.get(0).getCompanyTeam().getLunchPrice());
            int dinnerPrice = records.isEmpty() ? 0
                    : (records.get(0).getCompanyTeam().getDinnerPrice() == null ? 0
                    : records.get(0).getCompanyTeam().getDinnerPrice());

            this.lunchAmount = this.lunchCount * lunchPrice;
            this.dinnerAmount = this.dinnerCount * dinnerPrice;
            this.totalAmount = this.lunchAmount + this.dinnerAmount;
        }
    }

    /**
     * 회사별 정산 데이터
     */
    @Getter
    public static class CompanySettlement {
        private Long companyId;
        private String companyName;
        private List<TeamSettlement> teams;
        private int totalLunchCount;    // 중식 총 인원
        private int totalDinnerCount;   // 석식 총 인원
        private int totalLunchAmount;   // 중식 총 금액
        private int totalDinnerAmount;  // 석식 총 금액
        private int totalAmount;        // 합계 금액

        public CompanySettlement(Long companyId, String companyName,
                                 List<MealRecord> records) {
            this.companyId = companyId;
            this.companyName = companyName;

            // 팀별로 그룹핑
            Map<String, List<MealRecord>> byTeam = records.stream()
                    .collect(Collectors.groupingBy(
                            r -> r.getCompanyTeam().getTeamName()
                    ));

            // 팀별 정산 생성
            this.teams = byTeam.entrySet().stream()
                    .map(e -> new TeamSettlement(e.getKey(), e.getValue()))
                    .collect(Collectors.toList());

            // 회사 합계 계산
            this.totalLunchCount = teams.stream()
                    .mapToInt(TeamSettlement::getLunchCount).sum();
            this.totalDinnerCount = teams.stream()
                    .mapToInt(TeamSettlement::getDinnerCount).sum();
            this.totalLunchAmount = teams.stream()
                    .mapToInt(TeamSettlement::getLunchAmount).sum();
            this.totalDinnerAmount = teams.stream()
                    .mapToInt(TeamSettlement::getDinnerAmount).sum();
            this.totalAmount = this.totalLunchAmount + this.totalDinnerAmount;
        }
    }

    // ========================
    // API
    // ========================

    /**
     * 월별 정산 조회 API
     * GET /api/settlement?yearMonth=2026-04&companyId=1(선택)
     *
     * yearMonth: yyyy-MM 형식 (예: 2026-04)
     * companyId: 선택값, 없으면 전체 조회
     *
     * 응답 예시:
     * [
     *   {
     *     "companyId": 1,
     *     "companyName": "대박유통",
     *     "teams": [
     *       {
     *         "teamName": "개발팀",
     *         "lunchCount": 100,
     *         "dinnerCount": 50,
     *         "lunchAmount": 650000,
     *         "dinnerAmount": 350000,
     *         "totalAmount": 1000000
     *       }
     *     ],
     *     "totalAmount": 1000000
     *   }
     * ]
     */
    @GetMapping
    public ResponseEntity<List<CompanySettlement>> getSettlement(
            @RequestParam String yearMonth,
            @RequestParam(required = false) Long companyId) {

        // yearMonth (yyyy-MM) → 해당 월의 시작일/종료일 계산
        YearMonth ym = YearMonth.parse(yearMonth);
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();

        // 식사 기록 조회
        List<MealRecord> records;
        if (companyId != null) {
            // 특정 회사만 조회
            records = mealRecordService
                    .getMealRecordsByCompanyAndDateRange(companyId, startDate, endDate);
        } else {
            // 전체 조회
            records = mealRecordService
                    .getMealRecordsByDateRange(startDate, endDate);
        }

        // 회사별로 그룹핑
        Map<Long, List<MealRecord>> byCompany = records.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getCompany().getId()
                ));

        // 회사별 정산 생성
        List<CompanySettlement> result = byCompany.entrySet().stream()
                .map(e -> new CompanySettlement(
                        e.getKey(),
                        e.getValue().get(0).getCompany().getCompanyName(),
                        e.getValue()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }
}