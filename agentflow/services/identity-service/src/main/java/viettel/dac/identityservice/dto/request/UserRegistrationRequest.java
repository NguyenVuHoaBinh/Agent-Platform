package viettel.dac.identityservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import viettel.dac.identityservice.common.constants.SecurityConstants;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserRegistrationRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = SecurityConstants.MIN_PASSWORD_LENGTH,
            max = SecurityConstants.MAX_PASSWORD_LENGTH,
            message = "Password must be between " + SecurityConstants.MIN_PASSWORD_LENGTH +
                    " and " + SecurityConstants.MAX_PASSWORD_LENGTH + " characters")
    private String password;

    @Size(max = 50, message = "First name must be less than 50 characters")
    private String firstName;

    @Size(max = 50, message = "Last name must be less than 50 characters")
    private String lastName;

    private String clientIp;

    private String userAgent;
}