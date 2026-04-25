package meal_management.service;

import lombok.RequiredArgsConstructor;
import meal_management.entity.Company;
import meal_management.entity.CompanyTeam;
import meal_management.repository.CompanyTeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * 팀 관련 비즈니스 로직을 담당하는 서비스예요.
 * 팀 목록 조회, 등록, 수정 기능을 처리해요.
 */
@Service
@RequiredArgsConstructor
public class CompanyTeamService {

    private final CompanyTeamRepository companyTeamRepository;
    private final CompanyService companyService;

    /**
     * 특정 회사의 활성화된 팀 목록 조회
     * 회사 ID로 해당 회사의 팀 목록을 반환해요.
     */
    public List<CompanyTeam> getTeamsByCompany(Long companyId) {
        return companyTeamRepository.findByCompanyIdAndIsActiveTrue(companyId);
    }

    /**
     * 팀 단건 조회
     * 없으면 예외를 발생시켜요.
     */
    public CompanyTeam getTeam(Long id) {
        return companyTeamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("팀을 찾을 수 없습니다."));
    }

    /**
     * 팀 등록
     * 회사 ID로 회사를 조회한 후 팀을 등록해요.
     * 중식/석식 단가도 함께 저장해요.
     */
    @Transactional
    public CompanyTeam createTeam(Long companyId, CompanyTeam team) {
        // 회사 존재 여부 확인
        Company company = companyService.getCompany(companyId);

        // 팀에 회사 정보 설정
        team.setCompany(company);
        team.setIsActive(true);

        return companyTeamRepository.save(team);
    }

    /**
     * 팀 수정
     * 팀명, 중식 단가, 석식 단가를 수정해요.
     */
    @Transactional
    public CompanyTeam updateTeam(Long id, CompanyTeam updatedTeam) {
        // 기존 팀 조회
        CompanyTeam team = getTeam(id);

        // 변경된 값만 업데이트
        team.setTeamName(updatedTeam.getTeamName());
        team.setLunchPrice(updatedTeam.getLunchPrice());
        team.setDinnerPrice(updatedTeam.getDinnerPrice());

        return companyTeamRepository.save(team);
    }

    /**
     * 팀 소프트 딜리트
     * 실제로 DB에서 삭제하지 않고 is_active를 false로 변경해요.
     */
    @Transactional
    public void deleteTeam(Long id) {
        CompanyTeam team = getTeam(id);
        team.setIsActive(false);
        companyTeamRepository.save(team);
    }
}