package viettel.dac.identityservice.security;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Collections;
import java.util.Map;

/**
 * Result object for token validation
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TokenValidationResult {

    private final boolean valid;
    private final String userId;
    private final Map<String, Object> claims;
    private final String errorMessage;

    /**
     * Creates a valid result
     *
     * @param userId The user ID from the token
     * @param claims The token claims
     * @return A valid TokenValidationResult
     */
    public static TokenValidationResult valid(String userId, Map<String, Object> claims) {
        return new TokenValidationResult(true, userId, claims, null);
    }

    /**
     * Creates an invalid result
     *
     * @param errorMessage The error message
     * @return An invalid TokenValidationResult
     */
    public static TokenValidationResult invalid(String errorMessage) {
        return new TokenValidationResult(false, null, Collections.emptyMap(), errorMessage);
    }
}