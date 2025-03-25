package viettel.dac.identityservice.service;

import viettel.dac.identityservice.dto.request.AuthenticationRequest;
import viettel.dac.identityservice.dto.request.MfaVerificationRequest;
import viettel.dac.identityservice.dto.request.RefreshTokenRequest;
import viettel.dac.identityservice.dto.request.UserRegistrationRequest;
import viettel.dac.identityservice.dto.response.AuthenticationResponse;
import viettel.dac.identityservice.dto.response.UserResponse;
import viettel.dac.identityservice.entity.User;

/**
 * Service for handling authentication-related operations
 */
public interface AuthenticationService {

    /**
     * Authenticates a user with username/email and password
     *
     * @param request Authentication request with credentials
     * @return Authentication response with tokens or MFA challenge
     */
    AuthenticationResponse authenticate(AuthenticationRequest request);

    /**
     * Verifies a multi-factor authentication code
     *
     * @param request MFA verification request
     * @return Authentication response with tokens
     */
    AuthenticationResponse verifyMfa(MfaVerificationRequest request);

    /**
     * Refreshes an access token using a refresh token
     *
     * @param request Refresh token request
     * @return Authentication response with new tokens
     */
    AuthenticationResponse refreshToken(RefreshTokenRequest request);

    /**
     * Logs out a user by invalidating their tokens
     *
     * @param userId User ID
     * @param accessToken Access token to revoke
     * @param refreshToken Refresh token to revoke
     */
    void logout(String userId, String accessToken, String refreshToken);

    /**
     * Registers a new user
     *
     * @param request User registration request
     * @return Created user information
     */
    UserResponse registerUser(UserRegistrationRequest request);

    /**
     * Verifies a user's email using a verification token
     *
     * @param token Verification token
     * @return True if verification was successful
     */
    boolean verifyEmail(String token);

    /**
     * Creates and sends a password reset token
     *
     * @param email User's email
     * @return True if token was created and sent
     */
    boolean createPasswordResetToken(String email);

    /**
     * Validates a password reset token
     *
     * @param token Reset token
     * @return True if token is valid
     */
    boolean validatePasswordResetToken(String token);

    /**
     * Resets a user's password using a reset token
     *
     * @param token Reset token
     * @param newPassword New password
     * @return True if password was reset
     */
    boolean resetPassword(String token, String newPassword);
}