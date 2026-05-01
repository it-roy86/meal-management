package meal_management.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    // 비밀키 (32자 이상이어야 해요)
    private final String SECRET_KEY = "meal-management-secret-key-2026-roy86!!";

    // 토큰 유효시간 (24시간)
    private final long EXPIRATION = 1000 * 60 * 60 * 24;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }

    /**
     * 토큰 생성 (일반 사용자용)
     * username, role을 토큰에 담아요.
     */
    public String generateToken(String username, String role) {
        return Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 토큰 생성 (VIEWER용)
     * username, role, companyId를 토큰에 담아요.
     * VIEWER는 자기 회사 데이터만 조회할 수 있어요.
     */
    public String generateViewerToken(String username, Long companyId) {
        return Jwts.builder()
                .setSubject(username)
                .claim("role", "VIEWER")
                .claim("companyId", companyId) // 회사 ID 추가
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 토큰에서 companyId 추출 (VIEWER용)
     */
    public Long getCompanyIdFromToken(String token) {
        Object companyId = getClaims(token).get("companyId");
        if (companyId == null) return null;
        return Long.valueOf(companyId.toString());
    }

    // 토큰에서 사용자명 추출
    public String getUsernameFromToken(String token) {
        return getClaims(token).getSubject();
    }

    // 토큰에서 역할 추출
    public String getRoleFromToken(String token) {
        return getClaims(token).get("role", String.class);
    }

    // 토큰 유효성 검사
    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}