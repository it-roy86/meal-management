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

        String token = resolveToken(request);

        if (token != null && jwtUtil.validateToken(token)) {
            String username = jwtUtil.getUsernameFromToken(token);
            String role = jwtUtil.getRoleFromToken(token);
            Long companyId = jwtUtil.getCompanyIdFromToken(token); // 추가

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            username,
                            companyId, // credentials에 companyId 저장
                            List.of(new SimpleGrantedAuthority("ROLE_" + role))
                    );

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

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