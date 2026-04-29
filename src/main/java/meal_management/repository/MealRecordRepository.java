package meal_management.repository;

import meal_management.entity.MealRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

/**
 * 식사 기록 Repository
 */
public interface MealRecordRepository extends JpaRepository<MealRecord, Long> {

    /**
     * 날짜별 식사 기록 조회
     * WHERE record_date = ? 쿼리가 자동 생성돼요.
     */
    List<MealRecord> findByRecordDate(LocalDate recordDate);

    /**
     * 회사 + 날짜 범위 식사 기록 조회 (월별 정산용)
     * WHERE company_id = ? AND record_date BETWEEN ? AND ?
     */
    List<MealRecord> findByCompanyIdAndRecordDateBetween(
            Long companyId, LocalDate startDate, LocalDate endDate);
}