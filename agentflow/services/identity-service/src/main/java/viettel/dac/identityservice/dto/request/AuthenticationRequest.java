package viettel.dac.identityservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticationRequest {

    @NotBlank(message = "Email or username is required")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    private String clientIp;

    private String userAgent;
}