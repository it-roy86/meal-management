package meal_management;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

// UserDetailsServiceAutoConfiguration 제외
// Spring Boot가 자동으로 생성하는 기본 보안 설정과 충돌을 방지해요
@SpringBootApplication(exclude = {UserDetailsServiceAutoConfiguration.class})
public class MealManagementApplication {
	public static void main(String[] args) {
		SpringApplication.run(MealManagementApplication.class, args);
	}
}