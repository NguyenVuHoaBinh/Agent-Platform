package viettel.dac.identityservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import viettel.dac.identityservice.entity.User.UserStatus;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {

    private String id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private UserStatus status;
    private boolean emailVerified;
    private boolean mfaEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
}