package meal_management.util;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

/**
 * JWT 인증 필터
 * 모든 API 요청이 들어올 때마다 실행되는 경비원 역할이에요.
 * 요청 헤더에 JWT 토큰이 있으면 꺼내서 유효한지 확인하고
 * 유효하면 Spring Security에 "이 사람 인증됐어요!" 라고 알려줘요.
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil; // JWT 토큰 관련 기능을 담당하는 유틸

    /**
     * 실제 필터링 로직
     * 모든 HTTP 요청마다 이 메서드가 실행돼요.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // 1. 요청 헤더에서 JWT 토큰을 꺼내요
        String token = resolveToken(request);

        // 2. 토큰이 있고 유효하면 인증 처리를 해요
        if (token != null && jwtUtil.validateToken(token)) {

            // 토큰에서 사용자 정보를 꺼내요
            String username = jwtUtil.getUsernameFromToken(token);
            String role = jwtUtil.getRoleFromToken(token);

            // Spring Security에 인증 정보를 등록해요
            // "이 사람은 username이고 role 권한을 가지고 있어요!" 라고 알려주는 거예요
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            username,
                            null, // 비밀번호는 토큰 인증이라 필요 없어요
                            List.of(new SimpleGrantedAuthority("ROLE_" + role))
                            // ROLE_ADMIN, ROLE_OPERATOR, ROLE_VIEWER 형태로 등록돼요
                    );

            // SecurityContext에 인증 정보를 저장해요
            // 이후 컨트롤러에서 현재 로그인한 사람 정보를 꺼낼 수 있어요
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        // 3. 다음 필터로 요청을 넘겨요 (필터 체인 계속 진행)
        filterChain.doFilter(request, response);
    }

    /**
     * 요청 헤더에서 JWT 토큰을 꺼내는 메서드
     * Authorization 헤더 형식: "Bearer eyJhbGciOiJIUzI1NiJ9..."
     * "Bearer " 부분을 제거하고 순수한 토큰만 반환해요.
     */
    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7); // "Bearer " 7글자를 제거하고 반환
        }
        return null; // 토큰이 없으면 null 반환
    }
}