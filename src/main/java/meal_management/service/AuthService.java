package meal_management.service;

import lombok.RequiredArgsConstructor;
import meal_management.dto.LoginRequestDto;
import meal_management.dto.LoginResponseDto;
import meal_management.entity.Company;
import meal_management.entity.User;
import meal_management.repository.CompanyRepository;
import meal_management.repository.UserRepository;
import meal_management.util.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 로그인 관련 비즈니스 로직을 담당하는 서비스예요.
 * 아이디/비밀번호 확인 후 JWT 토큰을 발급해줘요.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final CompanyRepository companyRepository;

    /**
     * 로그인 처리 메서드
     * 1. 아이디로 사용자 조회
     * 2. 비밀번호 일치 여부 확인
     * 3. JWT 토큰 발급 후 반환
     */
    public LoginResponseDto login(LoginRequestDto requestDto) {

        // 1. 아이디로 사용자 조회 (없으면 예외 발생)
        User user = userRepository.findByUsername(requestDto.getUsername())
                .orElseThrow(() -> new RuntimeException("아이디 또는 비밀번호가 올바르지 않습니다."));

        // 2. 계정 활성화 여부 확인 (is_active = false면 로그인 불가)
        if (!user.getIsActive()) {
            throw new RuntimeException("비활성화된 계정입니다.");
        }

        // 3. 비밀번호 확인
        // 입력한 비밀번호와 DB에 암호화된 비밀번호를 비교해요
        if (!passwordEncoder.matches(requestDto.getPassword(), user.getPassword())) {
            throw new RuntimeException("아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        // 4. JWT 토큰 발급
        String token = jwtUtil.generateToken(
                user.getUsername(),
                user.getRole().name() // ADMIN, OPERATOR, VIEWER
        );

        // 5. 토큰 + 역할 + 사용자명 반환
        return new LoginResponseDto(token, user.getRole().name(), user.getUsername());
    }

    /**
     * VIEWER 로그인 처리
     * 회사 ID + 사업자번호 뒤 4자리로 인증해요.
     * 별도의 계정 없이 회사 정보만으로 로그인해요.
     */
    public LoginResponseDto viewerLogin(Long companyId, String businessNumberLast4) {

        // 1. 회사 조회
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("회사를 찾을 수 없습니다."));

        // 2. 활성화 여부 확인
        if (!company.getIsActive()) {
            throw new RuntimeException("비활성화된 회사입니다.");
        }

        // 3. 사업자번호 뒤 4자리 확인
        String businessNumber = company.getBusinessNumber();
        if (businessNumber == null || businessNumber.length() < 4) {
            throw new RuntimeException("사업자번호가 등록되지 않았습니다.");
        }

        String last4 = businessNumber.substring(businessNumber.length() - 4);
        if (!last4.equals(businessNumberLast4)) {
            throw new RuntimeException("사업자번호가 올바르지 않습니다.");
        }

        // 4. VIEWER JWT 토큰 발급 (username은 회사명으로 설정)
        String token = jwtUtil.generateViewerToken(
                "viewer_" + company.getCompanyName(),
                company.getId()
        );

        return new LoginResponseDto(token, "VIEWER", company.getCompanyName());
    }
}