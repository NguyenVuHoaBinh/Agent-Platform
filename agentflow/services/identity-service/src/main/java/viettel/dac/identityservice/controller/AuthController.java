package viettel.dac.identityservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import viettel.dac.identityservice.dto.*;
import viettel.dac.identityservice.model.User;
import viettel.dac.identityservice.security.UserDetailsImpl;
import viettel.dac.identityservice.service.AuthService;
import viettel.dac.identityservice.service.UserService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<JwtAuthenticationResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        JwtAuthenticationResponse response = authService.login(loginRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse> registerUser(@Valid @RequestBody SignUpRequest signUpRequest) {
        // Validate password confirmation if it was part of the request

        User user = authService.register(signUpRequest);

        UserDto userDto = UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse(true, "User registered successfully", userDto));
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser() {
        UserDetailsImpl currentUser = authService.getCurrentUser();

        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = userService.getUserById(currentUser.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserDto userDto = UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .enabled(user.isEnabled())
                .roles(user.getRoles().stream().map(role -> role.getName()).collect(java.util.stream.Collectors.toSet()))
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();

        return ResponseEntity.ok(userDto);
    }

    @PostMapping("/password/reset-request")
    public ResponseEntity<ApiResponse> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        boolean success = authService.initiatePasswordReset(request.getEmail());

        // Always return success for security (don't reveal if email exists)
        return ResponseEntity.ok(new ApiResponse(true,
                "If your email is registered, you will receive password reset instructions"));
    }

    @PostMapping("/password/reset-complete")
    public ResponseEntity<ApiResponse> completePasswordReset(@Valid @RequestBody PasswordResetCompleteRequest request) {
        // Validate password confirmation
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Password confirmation does not match"));
        }

        boolean success = authService.completePasswordReset(request.getToken(), request.getNewPassword());

        if (!success) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Invalid or expired password reset token"));
        }

        return ResponseEntity.ok(new ApiResponse(true, "Password has been reset successfully"));
    }

    @PostMapping("/validate-token")
    public ResponseEntity<ApiResponse> validateToken(@RequestParam String token) {
        boolean valid = authService.validateToken(token);

        if (!valid) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse(false, "Invalid or expired token"));
        }

        return ResponseEntity.ok(new ApiResponse(true, "Token is valid"));
    }
}