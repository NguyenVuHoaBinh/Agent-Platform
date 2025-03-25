package viettel.dac.identityservice.security;

import viettel.dac.identityservice.entity.User;

import java.util.Set;

/**
 * Service for handling JWT token generation and validation
 */
public interface JwtTokenProvider {

    /**
     * Generates a JWT access token for the provided user
     *
     * @param user The user for whom to generate the token
     * @param scopes The scopes to include in the token
     * @return The generated JWT access token
     */
    String generateAccessToken(User user, Set<String> scopes);

    /**
     * Generates a JWT refresh token for the provided user
     *
     * @param user The user for whom to generate the refresh token
     * @param accessTokenId The ID of the associated access token
     * @return The generated JWT refresh token
     */
    String generateRefreshToken(User user, String accessTokenId);

    /**
     * Generates a JWT MFA token for use during multi-factor authentication
     *
     * @param userId The ID of the user authenticating
     * @return The generated MFA token
     */
    String generateMfaToken(String userId);

    /**
     * Validates a JWT token
     *
     * @param token The token to validate
     * @return A result object containing validation status and token claims
     */
    TokenValidationResult validateToken(String token);

    /**
     * Gets the user ID from a token
     *
     * @param token The token to parse
     * @return The user ID from the token
     */
    String getUserIdFromToken(String token);

    /**
     * Gets the token ID (jti) from a token
     *
     * @param token The token to parse
     * @return The token ID
     */
    String getTokenId(String token);

    /**
     * Gets the token type (access, refresh, mfa)
     *
     * @param token The token to parse
     * @return The token type
     */
    String getTokenType(String token);

    /**
     * Revokes a token by adding it to the blacklist
     *
     * @param token The token to revoke
     */
    void revokeToken(String token);

    /**
     * Gets the access token expiration time in seconds
     *
     * @return Expiration time in seconds
     */
    long getAccessTokenExpiration();
}