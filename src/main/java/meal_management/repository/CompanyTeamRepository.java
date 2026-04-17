package meal_management.repository;

import meal_management.entity.CompanyTeam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CompanyTeamRepository extends JpaRepository<CompanyTeam, Long> {

    // 회사 ID로 활성화된 팀 목록 조회
    List<CompanyTeam> findByCompanyIdAndIsActiveTrue(Long companyId);

    // 전체 활성화된 팀 목록 조회
    List<CompanyTeam> findByIsActiveTrue();
}