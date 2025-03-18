package viettel.dac.identityservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Authentication", description = "Authentication API")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and generate JWT token",
            description = "Authenticates a user with username and password, and returns a JWT token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful authentication",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = JwtAuthenticationResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials",
                    content = @Content)
    })
    public ResponseEntity<JwtAuthenticationResponse> authenticateUser(
            @Parameter(description = "Login credentials", required = true)
            @Valid @RequestBody LoginRequest loginRequest) {
        JwtAuthenticationResponse response = authService.login(loginRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user",
            description = "Registers a new user with username, email, and password")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User successfully registered",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = viettel.dac.identityservice.dto.ApiResponse.class))),
            @ApiResponse(responseCode = "409", description = "Username or email already exists",
                    content = @Content)
    })
    public ResponseEntity<viettel.dac.identityservice.dto.ApiResponse> registerUser(
            @Parameter(description = "Registration details", required = true)
            @Valid @RequestBody SignUpRequest signUpRequest) {
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
                .body(new viettel.dac.identityservice.dto.ApiResponse(true, "User registered successfully", userDto));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user",
            description = "Returns the details of the currently authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved user details",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UserDto.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content)
    })
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
    @Operation(summary = "Request password reset",
            description = "Initiates the password reset process for the specified email")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password reset initiated",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = viettel.dac.identityservice.dto.ApiResponse.class)))
    })
    public ResponseEntity<viettel.dac.identityservice.dto.ApiResponse> requestPasswordReset(
            @Parameter(description = "Password reset request", required = true)
            @Valid @RequestBody PasswordResetRequest request) {
        boolean success = authService.initiatePasswordReset(request.getEmail());

        // Always return success for security (don't reveal if email exists)
        return ResponseEntity.ok(new viettel.dac.identityservice.dto.ApiResponse(true,
                "If your email is registered, you will receive password reset instructions"));
    }

    @PostMapping("/password/reset-complete")
    @Operation(summary = "Complete password reset",
            description = "Completes the password reset process using a token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password successfully reset",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = viettel.dac.identityservice.dto.ApiResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid token or password mismatch",
                    content = @Content)
    })
    public ResponseEntity<viettel.dac.identityservice.dto.ApiResponse> completePasswordReset(
            @Parameter(description = "Password reset completion details", required = true)
            @Valid @RequestBody PasswordResetCompleteRequest request) {
        // Validate password confirmation
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return ResponseEntity.badRequest()
                    .body(new viettel.dac.identityservice.dto.ApiResponse(false, "Password confirmation does not match"));
        }

        boolean success = authService.completePasswordReset(request.getToken(), request.getNewPassword());

        if (!success) {
            return ResponseEntity.badRequest()
                    .body(new viettel.dac.identityservice.dto.ApiResponse(false, "Invalid or expired password reset token"));
        }

        return ResponseEntity.ok(new viettel.dac.identityservice.dto.ApiResponse(true, "Password has been reset successfully"));
    }

    @PostMapping("/validate-token")
    @Operation(summary = "Validate JWT token",
            description = "Validates a JWT token and returns whether it is valid")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token is valid",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = viettel.dac.identityservice.dto.ApiResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token is invalid",
                    content = @Content)
    })
    public ResponseEntity<viettel.dac.identityservice.dto.ApiResponse> validateToken(
            @Parameter(description = "JWT token to validate", required = true)
            @RequestParam String token) {
        boolean valid = authService.validateToken(token);

        if (!valid) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new viettel.dac.identityservice.dto.ApiResponse(false, "Invalid or expired token"));
        }

        return ResponseEntity.ok(new viettel.dac.identityservice.dto.ApiResponse(true, "Token is valid"));
    }
}