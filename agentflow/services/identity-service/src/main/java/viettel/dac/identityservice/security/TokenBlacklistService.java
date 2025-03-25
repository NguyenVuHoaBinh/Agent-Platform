package viettel.dac.identityservice.security;

import java.util.Date;

/**
 * Service for managing blacklisted tokens
 */
public interface TokenBlacklistService {

    /**
     * Adds a token to the blacklist
     *
     * @param tokenId the ID of the token
     * @param expiration the expiration date of the token
     */
    void blacklistToken(String tokenId, Date expiration);

    /**
     * Checks if a token is blacklisted
     *
     * @param token the token to check
     * @return true if the token is blacklisted, false otherwise
     */
    boolean isBlacklisted(String token);
}