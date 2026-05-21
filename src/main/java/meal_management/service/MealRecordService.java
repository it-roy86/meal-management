package meal_management.service;

import lombok.RequiredArgsConstructor;
import meal_management.entity.Company;
import meal_management.entity.CompanyTeam;
import meal_management.entity.MealRecord;
import meal_management.entity.User;
import meal_management.repository.MealRecordRepository;
import meal_management.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

/**
 * 식사 기록 관련 비즈니스 로직을 담당하는 서비스예요.
 * 일일 식사 인원 입력 및 조회 기능을 처리해요.
 */
@Service
@RequiredArgsConstructor
public class MealRecordService {

    private final MealRecordRepository mealRecordRepository;
    private final CompanyService companyService;
    private final CompanyTeamService companyTeamService;
    private final UserRepository userRepository;

    /**
     * 식사 기록 저장
     * 회사/팀 정보로 단가를 가져와서 총 금액을 자동 계산해요.
     */
    @Transactional
    public MealRecord createMealRecord(
            LocalDate recordDate,
            Long companyId,
            Long companyTeamId,
            Integer lunchCount,
            Integer dinnerCount) {

        // 회사, 팀 정보 조회
        Company company = companyService.getCompany(companyId);
        CompanyTeam team = companyTeamService.getTeam(companyTeamId);

        // 현재 로그인한 사용자 조회
        String username = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // 총 인원 및 금액 계산
        int totalCount = (lunchCount == null ? 0 : lunchCount)
                + (dinnerCount == null ? 0 : dinnerCount);
        int lunchAmount = (lunchCount == null ? 0 : lunchCount)
                * (team.getLunchPrice() == null ? 0 : team.getLunchPrice());
        int dinnerAmount = (dinnerCount == null ? 0 : dinnerCount)
                * (team.getDinnerPrice() == null ? 0 : team.getDinnerPrice());
        int totalAmount = lunchAmount + dinnerAmount;

        // 식사 기록 생성
        MealRecord mealRecord = new MealRecord();
        mealRecord.setRecordDate(recordDate);
        mealRecord.setCompany(company);
        mealRecord.setCompanyTeam(team);
        mealRecord.setLunchCount(lunchCount == null ? 0 : lunchCount);
        mealRecord.setDinnerCount(dinnerCount == null ? 0 : dinnerCount);
        mealRecord.setTotalCount(totalCount);
        mealRecord.setTotalAmount(totalAmount);
        mealRecord.setCreatedBy(user);

        return mealRecordRepository.save(mealRecord);
    }

    /**
     * 특정 날짜의 식사 기록 목록 조회
     */
    public List<MealRecord> getMealRecordsByDate(LocalDate recordDate) {
        return mealRecordRepository.findByRecordDate(recordDate);
    }

    /**
     * 특정 회사의 날짜 범위 식사 기록 조회 (월별 정산용)
     */
    public List<MealRecord> getMealRecordsByCompanyAndDateRange(
            Long companyId, LocalDate startDate, LocalDate endDate) {
        return mealRecordRepository
                .findByCompanyIdAndRecordDateBetween(companyId, startDate, endDate);
    }

    /**
     * 날짜 범위 전체 식사 기록 조회
     */
    public List<MealRecord> getMealRecordsByDateRange(
            LocalDate startDate, LocalDate endDate) {
        return mealRecordRepository.findByRecordDateBetween(startDate, endDate);
    }

    /**
     * 식사 기록 수정
     * 중식/석식 인원을 수정하고 총 인원/금액을 다시 계산해요.
     */
    @Transactional
    public MealRecord updateMealRecord(Long id, Integer lunchCount, Integer dinnerCount) {

        // 기존 식사 기록 조회
        MealRecord mealRecord = mealRecordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("식사 기록을 찾을 수 없습니다."));

        // 팀 단가 가져오기
        int lunchPrice = mealRecord.getCompanyTeam().getLunchPrice() == null
                ? 0 : mealRecord.getCompanyTeam().getLunchPrice();
        int dinnerPrice = mealRecord.getCompanyTeam().getDinnerPrice() == null
                ? 0 : mealRecord.getCompanyTeam().getDinnerPrice();

        // 인원 업데이트
        int newLunchCount = lunchCount == null ? 0 : lunchCount;
        int newDinnerCount = dinnerCount == null ? 0 : dinnerCount;

        // 총 인원/금액 재계산
        int totalCount = newLunchCount + newDinnerCount;
        int totalAmount = (newLunchCount * lunchPrice) + (newDinnerCount * dinnerPrice);

        // 값 업데이트
        mealRecord.setLunchCount(newLunchCount);
        mealRecord.setDinnerCount(newDinnerCount);
        mealRecord.setTotalCount(totalCount);
        mealRecord.setTotalAmount(totalAmount);

        return mealRecordRepository.save(mealRecord);
    }
}