package meal_management;

import lombok.RequiredArgsConstructor;
import meal_management.entity.User;
import meal_management.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 서버 시작 시 테스트 계정을 자동으로 생성해요.
 * 이미 계정이 있으면 중복 생성하지 않아요.
 */
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {

        // admin 계정이 없을 때만 생성
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            // PasswordEncoder로 "1234"를 올바르게 암호화해요
            admin.setPassword(passwordEncoder.encode("1234"));
            admin.setRole(User.Role.ADMIN);
            admin.setIsActive(true);
            userRepository.save(admin);
            System.out.println("✅ admin 계정 생성 완료");
        }

        // operator 계정이 없을 때만 생성
        if (!userRepository.existsByUsername("operator")) {
            User operator = new User();
            operator.setUsername("operator");
            operator.setPassword(passwordEncoder.encode("1234"));
            operator.setRole(User.Role.OPERATOR);
            operator.setIsActive(true);
            userRepository.save(operator);
            System.out.println("✅ operator 계정 생성 완료");
        }

        // viewer 계정이 없을 때만 생성
        if (!userRepository.existsByUsername("viewer")) {
            User viewer = new User();
            viewer.setUsername("viewer");
            viewer.setPassword(passwordEncoder.encode("1234"));
            viewer.setRole(User.Role.VIEWER);
            viewer.setIsActive(true);
            userRepository.save(viewer);
            System.out.println("✅ viewer 계정 생성 완료");
        }
    }
}