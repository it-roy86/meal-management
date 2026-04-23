package meal_management.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginResponseDto {
    private String token;
    private String role;
    private String username;

    public LoginResponseDto(String token, String role, String username) {
        this.token = token;
        this.role = role;
        this.username = username;
    }
}