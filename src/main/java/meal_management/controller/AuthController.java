package meal_management.controller;

import lombok.RequiredArgsConstructor;
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
}