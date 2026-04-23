package meal_management.repository;

import meal_management.entity.MealRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MealRecordRepository extends JpaRepository<MealRecord, Long> {

    // 날짜와 팀 ID로 식사 기록 조회 (수정할 때 사용)
    Optional<MealRecord> findByRecordDateAndCompanyTeamId(
            LocalDate recordDate, Long companyTeamId);

    // 날짜 범위로 식사 기록 조회
    List<MealRecord> findByRecordDateBetween(
            LocalDate startDate, LocalDate endDate);

    // 회사 ID와 날짜 범위로 조회 (VIEWER용)
    List<MealRecord> findByCompanyIdAndRecordDateBetween(
            Long companyId, LocalDate startDate, LocalDate endDate);
}