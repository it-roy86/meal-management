package meal_management.repository;

import meal_management.entity.CompanyTeam;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * 팀 Repository
 * JpaRepository를 상속받아 기본 CRUD가 자동으로 생성돼요.
 */
public interface CompanyTeamRepository extends JpaRepository<CompanyTeam, Long> {

    /**
     * 특정 회사의 활성화된 팀 목록 조회
     * WHERE company_id = ? AND is_active = true 쿼리가 자동 생성돼요.
     */
    List<CompanyTeam> findByCompanyIdAndIsActiveTrue(Long companyId);
}