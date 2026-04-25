package meal_management.service;

import lombok.RequiredArgsConstructor;
import meal_management.entity.Company;
import meal_management.repository.CompanyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * 회사 관련 비즈니스 로직을 담당하는 서비스예요.
 * 회사 목록 조회, 등록, 수정 기능을 처리해요.
 */
@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;

    /**
     * 활성화된 회사 목록 전체 조회
     * is_active = true인 회사만 반환해요.
     */
    public List<Company> getActiveCompanies() {
        return companyRepository.findByIsActiveTrue();
    }

    /**
     * 회사 단건 조회
     * 없으면 예외를 발생시켜요.
     */
    public Company getCompany(Long id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("회사를 찾을 수 없습니다."));
    }

    /**
     * 회사 등록
     * 새 회사를 DB에 저장해요.
     * is_active는 기본값 true로 설정돼요.
     */
    @Transactional
    public Company createCompany(Company company) {
        company.setIsActive(true);
        return companyRepository.save(company);
    }

    /**
     * 회사 수정
     * 회사명, 사업자번호, 이메일을 수정해요.
     */
    @Transactional
    public Company updateCompany(Long id, Company updatedCompany) {
        // 기존 회사 조회
        Company company = getCompany(id);

        // 변경된 값만 업데이트
        company.setCompanyName(updatedCompany.getCompanyName());
        company.setBusinessNumber(updatedCompany.getBusinessNumber());
        company.setContactEmail(updatedCompany.getContactEmail());

        return companyRepository.save(company);
    }

    /**
     * 회사 소프트 딜리트
     * 실제로 DB에서 삭제하지 않고 is_active를 false로 변경해요.
     * 데이터는 유지하면서 화면에서만 안 보이게 해요.
     */
    @Transactional
    public void deleteCompany(Long id) {
        Company company = getCompany(id);
        company.setIsActive(false);
        companyRepository.save(company);
    }
}