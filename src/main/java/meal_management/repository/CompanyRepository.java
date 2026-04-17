package meal_management.repository;

import meal_management.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {

    // 활성화된 회사 목록 조회
    List<Company> findByIsActiveTrue();

    // 회사명으로 조회
    Company findByCompanyName(String companyName);
}