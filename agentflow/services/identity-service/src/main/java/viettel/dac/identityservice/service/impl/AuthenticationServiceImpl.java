package viettel.dac.identityservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import viettel.dac.identityservice.common.exception.AccountLockedException;
import viettel.dac.identityservice.common.exception.AuthenticationException;
import viettel.dac.identityservice.common.exception.InvalidTokenException;
import viettel.dac.identityservice.common.exception.ResourceNotFoundException;
import viettel.dac.identityservice.dto.request.AuthenticationRequest;
import viettel.dac.identityservice.dto.request.MfaVerificationRequest;
import viettel.dac.identityservice.dto.request.RefreshTokenRequest;
import viettel.dac.identityservice.dto.request.UserRegistrationRequest;
import viettel.dac.identityservice.dto.response.AuthenticationResponse;
import viettel.dac.identityservice.dto.response.UserResponse;
import viettel.dac.identityservice.entity.RefreshToken;
import viettel.dac.identityservice.entity.User;
import viettel.dac.identityservice.entity.VerificationToken;
import viettel.dac.identityservice.repository.RefreshTokenRepository;
import viettel.dac.identityservice.repository.UserRepository;
import viettel.dac.identityservice.repository.VerificationTokenRepository;
import viettel.dac.identityservice.security.JwtTokenProvider;
import viettel.dac.identityservice.security.TokenValidationResult;
import viettel.dac.identityservice.service.AuditService;
import viettel.dac.identityservice.service.AuthenticationService;
import viettel.dac.identityservice.service.LoginAttemptService;
import viettel.dac.identityservice.service.UserService;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserRepository userRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final LoginAttemptService loginAttemptService;
    private final AuditService auditService;
    private final UserService userService;

    @Override
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        // Check for too many failed attempts
        if (loginAttemptService.isBlocked(request.getEmail())) {
            auditService.logAuthEvent(null, "authentication.blocked", request.getClientIp(),
                    Map.of("email", request.getEmail(), "reason", "too_many_attempts"));
            throw new AccountLockedException("Account temporarily locked due to too many failed attempts");
        }

        // Find user by email or username
        User user = userRepository.findByEmail(request.getEmail())
                .orElseGet(() -> userRepository.findByUsername(request.getEmail())
                        .orElse(null));

        if (user == null) {
            // Record failed attempt but don't reveal user existence
            loginAttemptService.recordFailedAttempt(request.getEmail());
            auditService.logAuthEvent(null, "authentication.failed", request.getClientIp(),
                    Map.of("reason", "user_not_found"));
            throw new AuthenticationException("Invalid credentials");
        }

        // Check account status
        if (user.getStatus() != User.UserStatus.ACTIVE) {
            auditService.logAuthEvent(user.getId(), "authentication.failed", request.getClientIp(),
                    Map.of("reason", "account_" + user.getStatus().toString().toLowerCase()));
            throw new AuthenticationException("Account is " + user.getStatus().toString().toLowerCase());
        }

        // Validate password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            loginAttemptService.recordFailedAttempt(request.getEmail());
            auditService.logAuthEvent(user.getId(), "authentication.failed", request.getClientIp(),
                    Map.of("reason", "invalid_password"));
            throw new AuthenticationException("Invalid credentials");
        }

        // Reset failed attempts on successful password verification
        loginAttemptService.resetFailedAttempts(request.getEmail());

        // If MFA is enabled, return challenge
        if (user.isMfaEnabled()) {
            String mfaToken = tokenProvider.generateMfaToken(user.getId());

            auditService.logAuthEvent(user.getId(), "authentication.mfa_required", request.getClientIp(),
                    Map.of("mfa_type", "totp"));

            return AuthenticationResponse.builder()
                    .status(AuthenticationResponse.AuthStatus.REQUIRES_MFA)
                    .userId(user.getId())
                    .mfaToken(mfaToken)
                    .mfaType("totp")
                    .build();
        }

        // Generate tokens for authentication
        Set<String> scopes = Collections.emptySet(); // To be replaced with actual user scopes
        String accessToken = tokenProvider.generateAccessToken(user, scopes);
        String refreshToken = tokenProvider.generateRefreshToken(user, tokenProvider.getTokenId(accessToken));

        // Update last login
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // Log successful authentication
        auditService.logAuthEvent(user.getId(), "authentication.success", request.getClientIp(),
                Map.of("method", "password"));

        return AuthenticationResponse.builder()
                .status(AuthenticationResponse.AuthStatus.AUTHENTICATED)
                .userId(user.getId())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getAccessTokenExpiration())
                .build();
    }

    @Override
    public AuthenticationResponse verifyMfa(MfaVerificationRequest request) {
        // Validate MFA token
        TokenValidationResult tokenValidation = tokenProvider.validateToken(request.getMfaToken());
        if (!tokenValidation.isValid()) {
            throw new InvalidTokenException("Invalid MFA session: " + tokenValidation.getErrorMessage());
        }

        String userId = tokenValidation.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // TODO: Implement proper TOTP verification once the TOTP service is ready
        // For now, just simulate a successful verification with a dummy code "123456"
        boolean validTotp = "123456".equals(request.getCode());
        if (!validTotp) {
            auditService.logAuthEvent(userId, "authentication.mfa_failed", request.getClientIp(),
                    Map.of("mfa_type", "totp"));
            throw new AuthenticationException("Invalid verification code");
        }

        // Generate tokens after successful MFA
        Set<String> scopes = Collections.emptySet(); // To be replaced with actual user scopes
        String accessToken = tokenProvider.generateAccessToken(user, scopes);
        String refreshToken = tokenProvider.generateRefreshToken(user, tokenProvider.getTokenId(accessToken));

        // Update last login
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // Log successful MFA
        auditService.logAuthEvent(userId, "authentication.success", request.getClientIp(),
                Map.of("method", "password_with_mfa"));

        return AuthenticationResponse.builder()
                .status(AuthenticationResponse.AuthStatus.AUTHENTICATED)
                .userId(user.getId())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getAccessTokenExpiration())
                .build();
    }

    @Override
    public AuthenticationResponse refreshToken(RefreshTokenRequest request) {
        // Validate refresh token
        TokenValidationResult validationResult = tokenProvider.validateToken(request.getRefreshToken());
        if (!validationResult.isValid()) {
            throw new InvalidTokenException("Invalid refresh token: " + validationResult.getErrorMessage());
        }

        String userId = validationResult.getUserId();
        String tokenId = tokenProvider.getTokenId(request.getRefreshToken());

        // Verify refresh token in database
        RefreshToken refreshToken = refreshTokenRepository.findByTokenId(tokenId)
                .orElseThrow(() -> new InvalidTokenException("Refresh token not found"));

        if (refreshToken.isRevoked()) {
            auditService.logAuthEvent(userId, "token.refresh.failed", request.getClientIp(),
                    Map.of("reason", "token_revoked"));
            throw new InvalidTokenException("Refresh token has been revoked");
        }

        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            auditService.logAuthEvent(userId, "token.refresh.failed", request.getClientIp(),
                    Map.of("reason", "token_expired"));
            throw new InvalidTokenException("Refresh token has expired");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Check user status
        if (user.getStatus() != User.UserStatus.ACTIVE) {
            auditService.logAuthEvent(userId, "token.refresh.failed", request.getClientIp(),
                    Map.of("reason", "account_" + user.getStatus().toString().toLowerCase()));
            throw new AuthenticationException("Account is " + user.getStatus().toString().toLowerCase());
        }

        // Revoke the old refresh token
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        // Generate new tokens
        Set<String> scopes = Collections.emptySet(); // To be replaced with actual user scopes
        String newAccessToken = tokenProvider.generateAccessToken(user, scopes);
        String newRefreshToken = tokenProvider.generateRefreshToken(user, tokenProvider.getTokenId(newAccessToken));

        // Log refresh
        auditService.logAuthEvent(userId, "token.refreshed", request.getClientIp(), Collections.emptyMap());

        return AuthenticationResponse.builder()
                .status(AuthenticationResponse.AuthStatus.AUTHENTICATED)
                .userId(user.getId())
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getAccessTokenExpiration())
                .build();
    }

    @Override
    public void logout(String userId, String accessToken, String refreshToken) {
        // Revoke access token
        if (accessToken != null) {
            tokenProvider.revokeToken(accessToken);
        }

        // Revoke refresh token
        if (refreshToken != null) {
            tokenProvider.revokeToken(refreshToken);
        }

        // Log logout
        auditService.logAuthEvent(userId, "authentication.logout", null, Collections.emptyMap());
    }

    @Override
    public UserResponse registerUser(UserRegistrationRequest request) {
        // User registration is delegated to the UserService
        return userService.registerUser(request);
    }

    @Override
    public boolean verifyEmail(String token) {
        // Find token
        VerificationToken verificationToken = verificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Invalid verification token"));

        // Check if token is valid
        if (!verificationToken.isValid()) {
            if (verificationToken.isExpired()) {
                throw new InvalidTokenException("Verification token has expired");
            } else {
                throw new InvalidTokenException("Verification token has already been used");
            }
        }

        // Check token type
        if (verificationToken.getTokenType() != VerificationToken.TokenType.EMAIL_VERIFICATION) {
            throw new InvalidTokenException("Invalid token type");
        }

        // Get user
        User user = userRepository.findById(verificationToken.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Update user
        user.setEmailVerified(true);

        // If user is in PENDING status, activate the account
        if (user.getStatus() == User.UserStatus.PENDING) {
            user.setStatus(User.UserStatus.ACTIVE);
        }

        userRepository.save(user);

        // Mark token as used
        verificationToken.setUsed(true);
        verificationToken.setUsedAt(LocalDateTime.now());
        verificationTokenRepository.save(verificationToken);

        // Log event
        auditService.logAuthEvent(user.getId(), "email.verified", null, Collections.emptyMap());

        return true;
    }

    @Override
    public boolean createPasswordResetToken(String email) {
        // Find user by email
        Optional<User> userOpt = userRepository.findByEmail(email);

        // For security reasons, always return true even if email doesn't exist
        if (userOpt.isEmpty()) {
            log.info("Password reset requested for non-existent email: {}", email);
            return true;
        }

        User user = userOpt.get();

        // Invalidate any existing password reset tokens
        verificationTokenRepository.invalidateAllUserTokensOfType(
                user.getId(),
                VerificationToken.TokenType.PASSWORD_RESET,
                LocalDateTime.now()
        );

        // Create new token
        String tokenValue = UUID.randomUUID().toString();
        VerificationToken passwordResetToken = VerificationToken.builder()
                .token(tokenValue)
                .userId(user.getId())
                .tokenType(VerificationToken.TokenType.PASSWORD_RESET)
                .expiresAt(LocalDateTime.now().plusHours(1)) // 1 hour expiry for password reset
                .used(false)
                .build();

        verificationTokenRepository.save(passwordResetToken);

        // Log event
        auditService.logAuthEvent(user.getId(), "password.reset.requested", null, Collections.emptyMap());

        // TODO: Send password reset email with token
        log.info("Password reset token created for user {}: {}", user.getId(), tokenValue);

        return true;
    }

    @Override
    public boolean validatePasswordResetToken(String token) {
        // Find token
        Optional<VerificationToken> tokenOpt = verificationTokenRepository.findByToken(token);

        if (tokenOpt.isEmpty()) {
            return false;
        }

        VerificationToken verificationToken = tokenOpt.get();

        // Check if token is valid
        if (!verificationToken.isValid()) {
            return false;
        }

        // Check token type
        return verificationToken.getTokenType() == VerificationToken.TokenType.PASSWORD_RESET;
    }

    @Override
    public boolean resetPassword(String token, String newPassword) {
        // Find token
        VerificationToken verificationToken = verificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Invalid reset token"));

        // Check if token is valid
        if (!verificationToken.isValid()) {
            if (verificationToken.isExpired()) {
                throw new InvalidTokenException("Reset token has expired");
            } else {
                throw new InvalidTokenException("Reset token has already been used");
            }
        }

        // Check token type
        if (verificationToken.getTokenType() != VerificationToken.TokenType.PASSWORD_RESET) {
            throw new InvalidTokenException("Invalid token type");
        }

        // Get user
        User user = userRepository.findById(verificationToken.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Update password
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Mark token as used
        verificationToken.setUsed(true);
        verificationToken.setUsedAt(LocalDateTime.now());
        verificationTokenRepository.save(verificationToken);

        // Revoke all refresh tokens for this user
        refreshTokenRepository.revokeAllUserTokens(user.getId());

        // Log event
        auditService.logAuthEvent(user.getId(), "password.reset.completed", null, Collections.emptyMap());

        return true;
    }
}