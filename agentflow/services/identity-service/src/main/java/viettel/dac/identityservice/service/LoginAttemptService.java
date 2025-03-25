package viettel.dac.identityservice.service;

/**
 * Service for tracking and limiting login attempts to prevent brute force attacks
 */
public interface LoginAttemptService {

    /**
     * Record a failed login attempt for the given username or email
     *
     * @param usernameOrEmail The username or email that failed to login
     */
    void recordFailedAttempt(String usernameOrEmail);

    /**
     * Reset the failed attempt counter for a username or email
     *
     * @param usernameOrEmail The username or email to reset
     */
    void resetFailedAttempts(String usernameOrEmail);

    /**
     * Check if a username or email is currently blocked due to too many failed attempts
     *
     * @param usernameOrEmail The username or email to check
     * @return true if blocked, false otherwise
     */
    boolean isBlocked(String usernameOrEmail);

    /**
     * Get the number of failed attempts for a username or email
     *
     * @param usernameOrEmail The username or email to check
     * @return The number of failed attempts
     */
    int getFailedAttempts(String usernameOrEmail);
}