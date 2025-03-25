package viettel.dac.identityservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticationResponse {

    private AuthStatus status;
    private String userId;
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long expiresIn;
    private String mfaToken;
    private String mfaType;

    public enum AuthStatus {
        AUTHENTICATED,
        REQUIRES_MFA,
        REQUIRES_PASSWORD_RESET,
        REQUIRES_ACCOUNT_ACTIVATION,
        FAILED
    }
}