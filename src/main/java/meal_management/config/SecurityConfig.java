package meal_management.config;

import lombok.RequiredArgsConstructor;
import meal_management.util.JwtAuthenticationFilter;
import meal_management.util.JwtUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
                // JWT 방식에서는 세션을 사용하지 않아서 CSRF 공격 위험이 없어요.
                .csrf(csrf -> csrf.disable())

                // 기본 로그인 폼 비활성화
                // 우리는 JWT 방식을 쓰기 때문에 Spring 기본 로그인 페이지가 필요 없어요.
                .formLogin(formLogin -> formLogin.disable())

                // HTTP Basic 인증 비활성화
                // 브라우저 기본 인증 팝업을 비활성화해요.
                .httpBasic(httpBasic -> httpBasic.disable())

                // CORS 설정 (Vue.js에서 API 호출 허용)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 세션 사용 안 함 (STATELESS)
                // JWT는 서버가 상태를 저장하지 않아요.
                // 매 요청마다 JWT 토큰으로 인증해요.
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // API별 접근 권한 설정
                .authorizeHttpRequests(auth -> auth

                        // OPTIONS 요청 허용 (CORS Preflight)
                        // 브라우저가 실제 요청 전에 OPTIONS 요청으로 허용 여부를 확인해요.
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 에러 페이지 허용
                        // 에러 발생 시 /error로 포워딩되는데 이것도 허용해야 해요.
                        .requestMatchers("/error").permitAll()

                        // 로그인 API 허용 (누구나 접근 가능)
                        // /api/auth/login, /api/auth/viewer-login 모두 포함돼요.
                        .requestMatchers("/api/auth/**").permitAll()

                        // 경리담당자 로그인 화면의 회사 목록 조회 허용
                        // 로그인 전에 회사 목록이 필요해서 인증 없이 접근 가능하게 해요.
                        .requestMatchers("/api/companies/public").permitAll()

                        // ADMIN만 접근 가능한 API
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // ADMIN, OPERATOR만 접근 가능한 API
                        .requestMatchers("/api/meal/input/**").hasAnyRole("ADMIN", "OPERATOR")

                        // 나머지는 로그인한 사람이면 모두 접근 가능
                        // JWT 토큰이 있어야 접근할 수 있어요.
                        .anyRequest().authenticated()
                )

                // JWT 필터를 Spring Security 필터 앞에 추가
                // 모든 요청마다 JWT 토큰을 먼저 검사해요.
                // 토큰이 유효하면 SecurityContext에 인증 정보를 저장해요.
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
     * 같은 비밀번호도 매번 다른 해시값이 생성돼요.
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

        // 허용할 HTTP 메서드
        // OPTIONS는 CORS Preflight 요청에 필요해요.
        config.setAllowedMethods(
                List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
        );

        // 허용할 헤더
        // Authorization 헤더에 JWT 토큰이 담겨요.
        config.setAllowedHeaders(List.of("*"));

        // 인증 정보 포함 허용
        // JWT 토큰을 헤더에 포함해서 보낼 수 있게 해요.
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}