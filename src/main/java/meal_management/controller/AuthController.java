package meal_management.controller;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import meal_management.dto.LoginRequestDto;
import meal_management.dto.LoginResponseDto;
import meal_management.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 인증 관련 API를 처리하는 컨트롤러예요.
 * Vue.js에서 로그인 요청이 오면 여기서 받아요.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

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
     * 응답 예시:
     * {
     *   "token": "eyJhbGciOiJIUzI1NiJ9...",
     *   "role": "ADMIN",
     *   "username": "admin"
     * }
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@RequestBody LoginRequestDto requestDto) {
        LoginResponseDto response = authService.login(requestDto);
        return ResponseEntity.ok(response);
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
            @RequestBody ViewerLoginRequest request) {
        LoginResponseDto response = authService.viewerLogin(
                request.getCompanyId(),
                request.getBusinessNumberLast4()
        );
        return ResponseEntity.ok(response);
    }
}