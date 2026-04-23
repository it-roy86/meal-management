package meal_management.config;

import org.springframework.http.HttpMethod;
import lombok.RequiredArgsConstructor;
import meal_management.util.JwtAuthenticationFilter;
import meal_management.util.JwtUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

/**
 * Spring Security 설정 클래스
 * 어떤 API는 누구나 접근 가능하고
 * 어떤 API는 특정 역할만 접근 가능한지 설정해요.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtil jwtUtil;

    /**
     * 보안 필터 체인 설정
     * API별 접근 권한을 여기서 설정해요.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 비활성화
                .csrf(csrf -> csrf.disable())

                // 기본 로그인 폼 비활성화 (우리는 JWT 쓸 거니까)
                .formLogin(formLogin -> formLogin.disable())

                // HTTP Basic 인증 비활성화
                .httpBasic(httpBasic -> httpBasic.disable())

                // CORS 설정
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 세션 사용 안 함
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // API별 접근 권한 설정
                .authorizeHttpRequests(auth -> auth
                        // OPTIONS 요청 허용 (CORS Preflight)
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // 에러 페이지 허용 (에러 발생 시 /error로 포워딩되는 것 허용)
                        .requestMatchers("/error").permitAll()
                        // 로그인 API는 누구나 접근 가능
                        .requestMatchers("/api/auth/**").permitAll()
                        // ADMIN만 접근 가능한 API
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // OPERATOR만 접근 가능한 API
                        .requestMatchers("/api/meal/input/**").hasAnyRole("ADMIN", "OPERATOR")
                        // 나머지는 로그인한 사람이면 모두 접근 가능
                        .anyRequest().authenticated()
                )

                // JWT 필터 추가
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtUtil),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    /**
     * 비밀번호 암호화 설정
     * BCrypt 방식으로 비밀번호를 안전하게 암호화해요.
     * DB에 비밀번호를 절대 평문으로 저장하면 안 돼요!
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * CORS 설정
     * Vue.js(localhost:5173)에서 Spring Boot(localhost:8080)로
     * API 요청을 보낼 수 있도록 허용해요.
     * 도메인이 다르면 브라우저가 기본적으로 요청을 막거든요!
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Vue.js 개발 서버 주소 허용
        config.setAllowedOrigins(List.of("http://localhost:5173"));

        // 허용할 HTTP 메서드 (OPTIONS 포함 - CORS Preflight 허용)
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        // 허용할 헤더
        config.setAllowedHeaders(List.of("*"));

        // 인증 정보 포함 허용
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}