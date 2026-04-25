package meal_management.repository;

import meal_management.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

/**
 * 회사 Repository
 * JpaRepository를 상속받아 기본 CRUD가 자동으로 생성돼요.
 * 추가로 필요한 조회 메서드를 메서드 이름 규칙으로 정의해요.
 */
public interface CompanyRepository extends JpaRepository<Company, Long> {

    /**
     * 활성화된 회사 목록 조회
     * is_active = true인 회사만 반환해요.
     * WHERE is_active = true 쿼리가 자동 생성돼요.
     */
    List<Company> findByIsActiveTrue();

    /**
     * 회사명으로 회사 존재 여부 확인
     * 회사명 중복 등록을 방지할 때 사용해요.
     * WHERE company_name = ? 쿼리가 자동 생성돼요.
     */
    boolean existsByCompanyName(String companyName);
}