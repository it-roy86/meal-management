package meal_management;

import lombok.RequiredArgsConstructor;
import meal_management.entity.User;
import meal_management.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 서버 시작 시 테스트 계정을 자동으로 생성해요.
 * 이미 계정이 있으면 중복 생성하지 않아요.
 * 비밀번호는 환경변수로 관리해요. (.env 파일)
 * 환경변수가 없으면 기본값을 사용해요.
 */
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // 환경변수에서 비밀번호 주입
    // 환경변수가 없으면 기본값 1234 사용
    @Value("${ADMIN_PASSWORD:1234}")
    private String adminPassword;

    @Value("${OPERATOR_PASSWORD:1234}")
    private String operatorPassword;

    @Override
    public void run(ApplicationArguments args) {

        // admin 계정이 없을 때만 생성
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            // 환경변수로 받은 비밀번호를 BCrypt로 암호화해요
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRole(User.Role.ADMIN);
            admin.setIsActive(true);
            userRepository.save(admin);
            System.out.println("✅ admin 계정 생성 완료");
        }

        // operator 계정이 없을 때만 생성
        if (!userRepository.existsByUsername("operator")) {
            User operator = new User();
            operator.setUsername("operator");
            operator.setPassword(passwordEncoder.encode(operatorPassword));
            operator.setRole(User.Role.OPERATOR);
            operator.setIsActive(true);
            userRepository.save(operator);
            System.out.println("✅ operator 계정 생성 완료");
        }
    }
}