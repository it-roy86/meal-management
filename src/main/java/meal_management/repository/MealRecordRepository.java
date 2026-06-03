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

    /**
     * 날짜 범위 전체 조회
     * WHERE record_date BETWEEN ? AND ?
     */
    List<MealRecord> findByRecordDateBetween(
            LocalDate startDate, LocalDate endDate);

    /**
     * 날짜 범위 전체 조회 (날짜 오름차순 정렬)
     * WHERE record_date BETWEEN ? AND ? ORDER BY record_date ASC
     * 식사 현황 조회 화면에서 날짜순으로 보여줄 때 사용해요.
     */
    List<MealRecord> findByRecordDateBetweenOrderByRecordDateAsc(
            LocalDate startDate, LocalDate endDate);

    /**
     * 회사 + 날짜 범위 조회 (날짜 오름차순 정렬)
     * WHERE company_id = ? AND record_date BETWEEN ? AND ? ORDER BY record_date ASC
     * VIEWER의 자기 회사 데이터 조회 및 ADMIN의 회사별 필터 조회에 사용해요.
     */
    List<MealRecord> findByCompanyIdAndRecordDateBetweenOrderByRecordDateAsc(
            Long companyId, LocalDate startDate, LocalDate endDate);
}