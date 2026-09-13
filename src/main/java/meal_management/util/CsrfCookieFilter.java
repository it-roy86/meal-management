package meal_management.util;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * CSRF 토큰 쿠키를 강제로 내려주는 필터
 *
 * Spring Security의 CsrfToken은 기본적으로 "누군가 실제로 값을 읽을 때"
 * 지연 계산(lazy)되는데, 그럼 SPA 입장에서는 처음에 아무 것도 읽지 않은
 * 요청에서는 XSRF-TOKEN 쿠키가 안 내려와요. 그래서 매 요청마다 토큰 값을
 * 한번 읽어줘서(getToken()) 쿠키가 항상 발급되도록 강제해요.
 * (Spring Security 공식 문서의 SPA용 CSRF 설정 예제와 동일한 방식이에요.)
 */
public class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain)
            throws ServletException, IOException {

        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            csrfToken.getToken(); // 값을 읽는 순간 XSRF-TOKEN 쿠키가 응답에 내려가요
        }

        filterChain.doFilter(request, response);
    }
}
