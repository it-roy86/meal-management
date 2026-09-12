package meal_management.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import meal_management.dto.LoginRequestDto;
import meal_management.dto.LoginResponseDto;
import meal_management.service.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

/**
 * 인증 관련 API를 처리하는 컨트롤러예요.
 * Vue.js에서 로그인 요청이 오면 여기서 받아요.
 *
 * JWT는 응답 본문이 아니라 httpOnly 쿠키로 내려줘요.
 * httpOnly 쿠키는 JS(document.cookie 등)로 읽을 수 없어서,
 * XSS 공격이 발생해도 토큰 자체를 훔쳐가지는 못해요.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String COOKIE_NAME = "token";

    private final AuthService authService;

    // 쿠키 Secure 속성 (HTTPS에서만 전송) 여부
    // 아직 HTTPS를 적용하지 않은 환경(로컬/현재 배포)이라 기본값은 false예요.
    // 운영 환경에 HTTPS를 적용하면 COOKIE_SECURE=true 환경변수로 켜야 해요.
    @Value("${COOKIE_SECURE:false}")
    private boolean cookieSecure;

    /**
     * 로그인 API
     * POST /api/auth/login
     *
     * 요청 예시:
     * {
     *   "username": "admin",
     *   "password": "1234"
     * }
     *
     * 응답 예시 (토큰은 본문이 아니라 Set-Cookie 헤더로 내려가요):
     * {
     *   "role": "ADMIN",
     *   "username": "admin"
     * }
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@RequestBody LoginRequestDto requestDto,
                                                   HttpServletResponse response) {
        LoginResponseDto result = authService.login(requestDto);
        setTokenCookie(response, result.getToken());
        result.setToken(null); // 응답 본문에는 토큰을 담지 않아요
        return ResponseEntity.ok(result);
    }

    /**
     * VIEWER 로그인 요청 DTO
     */
    @Getter
    @Setter
    public static class ViewerLoginRequest {
        private Long companyId;           // 회사 ID
        private String businessNumberLast4; // 사업자번호 뒤 4자리
    }

    /**
     * VIEWER 로그인 API
     * POST /api/auth/viewer-login
     *
     * 요청 예시:
     * {
     *   "companyId": 1,
     *   "businessNumberLast4": "7890"
     * }
     */
    @PostMapping("/viewer-login")
    public ResponseEntity<LoginResponseDto> viewerLogin(
            @RequestBody ViewerLoginRequest request,
            HttpServletResponse response) {
        LoginResponseDto result = authService.viewerLogin(
                request.getCompanyId(),
                request.getBusinessNumberLast4()
        );
        setTokenCookie(response, result.getToken());
        result.setToken(null); // 응답 본문에는 토큰을 담지 않아요
        return ResponseEntity.ok(result);
    }

    /**
     * 로그아웃 API
     * POST /api/auth/logout
     *
     * httpOnly 쿠키는 프론트에서 JS로 지울 수 없어서,
     * 서버가 만료된 쿠키를 다시 내려줘서 브라우저가 지우도록 해요.
     * (JWT 자체를 서버에서 무효화하는 건 아니고, 브라우저에서만 쿠키를 제거해요.
     * 자세한 내용은 CLAUDE.md '주의사항' 참고)
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.ok().build();
    }

    /**
     * JWT를 httpOnly 쿠키로 내려주는 공통 메서드
     * - httpOnly: JS로 접근 불가 (XSS로 토큰 탈취 방지)
     * - sameSite=Lax: 다른 사이트에서의 요청엔 쿠키가 자동으로 안 붙어서
     *   CSRF 공격을 완화해요 (그래서 SecurityConfig의 CSRF는 계속 비활성 상태로 둬요)
     * - secure: HTTPS 적용 후 true로 켜야 해요 (COOKIE_SECURE 환경변수)
     */
    private void setTokenCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, token)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofHours(24)) // JwtUtil의 토큰 만료시간(24시간)과 맞춤
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}