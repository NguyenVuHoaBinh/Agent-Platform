package viettel.dac.identityservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import viettel.dac.identityservice.dto.JwtAuthenticationResponse;
import viettel.dac.identityservice.dto.LoginRequest;
import viettel.dac.identityservice.dto.SignUpRequest;
import viettel.dac.identityservice.exception.EmailAlreadyExistsException;
import viettel.dac.identityservice.exception.ResourceNotFoundException;
import viettel.dac.identityservice.exception.UsernameAlreadyExistsException;
import viettel.dac.identityservice.model.Role;
import viettel.dac.identityservice.model.User;
import viettel.dac.identityservice.repository.RoleRepository;
import viettel.dac.identityservice.repository.UserRepository;
import viettel.dac.identityservice.security.JwtTokenProvider;
import viettel.dac.identityservice.security.UserDetailsImpl;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Authenticate a user and generate a JWT token
     *
     * @param loginRequest The login request
     * @return JWT authentication response
     */
    public JwtAuthenticationResponse login(LoginRequest loginRequest) {
        log.debug("Authenticating user: {}", loginRequest.getUsername());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);

        return new JwtAuthenticationResponse(jwt);
    }

    /**
     * Register a new user
     *
     * @param signUpRequest The sign-up request
     * @return The created user
     */
    @Transactional
    public User register(SignUpRequest signUpRequest) {
        log.debug("Registering new user: {}", signUpRequest.getUsername());

        // Validate username and email are available
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            throw new UsernameAlreadyExistsException("Username is already taken: " + signUpRequest.getUsername());
        }
        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            throw new EmailAlreadyExistsException("Email is already in use: " + signUpRequest.getEmail());
        }

        // Create new user's account
        User user = User.builder()
                .id(UUID.randomUUID().toString())
                .username(signUpRequest.getUsername())
                .email(signUpRequest.getEmail())
                .firstName(signUpRequest.getFirstName())
                .lastName(signUpRequest.getLastName())
                .passwordHash(passwordEncoder.encode(signUpRequest.getPassword()))
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .build();

        // Assign default user role
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new ResourceNotFoundException("Default user role not found"));
        user.addRole(userRole);

        return userRepository.save(user);
    }

    /**
     * Get the current authenticated user
     *
     * @return The current user
     */
    public UserDetailsImpl getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        if (authentication.getPrincipal() instanceof UserDetailsImpl) {
            return (UserDetailsImpl) authentication.getPrincipal();
        }

        return null;
    }

    /**
     * Validate a JWT token
     *
     * @param token The token to validate
     * @return True if the token is valid
     */
    public boolean validateToken(String token) {
        return tokenProvider.validateToken(token);
    }

    /**
     * Extract user ID from a JWT token
     *
     * @param token The JWT token
     * @return The user ID
     */
    public String getUserIdFromToken(String token) {
        return tokenProvider.getUserIdFromToken(token);
    }

    /**
     * Initiate password reset process
     *
     * @param email The user's email
     * @return True if reset initiated successfully
     */
    public boolean initiatePasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElse(null);

        if (user == null) {
            // Return true even if user not found for security reasons
            log.debug("Password reset requested for non-existent email: {}", email);
            return true;
        }

        // Generate reset token and send email
        // Implementation would normally generate a token, store it, and send an email with reset link
        log.debug("Password reset initiated for user: {}", user.getUsername());

        return true;
    }

    /**
     * Complete password reset process
     *
     * @param token The reset token
     * @param newPassword The new password
     * @return True if reset completed successfully
     */
    @Transactional
    public boolean completePasswordReset(String token, String newPassword) {
        // Validate token and find user
        // This is a placeholder - real implementation would validate the token and find the user

        User user = null; // In a real implementation, find user by valid reset token

        if (user == null) {
            log.debug("Invalid or expired password reset token: {}", token);
            return false;
        }

        // Update password
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.debug("Password reset completed for user: {}", user.getUsername());
        return true;
    }
}