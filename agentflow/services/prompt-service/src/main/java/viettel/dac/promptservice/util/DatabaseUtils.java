package viettel.dac.promptservice.util;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for database operations.
 * Provides methods for generating IDs, timestamps, and other database-related utilities.
 */
@Component
@Slf4j
public class DatabaseUtils {

    /**
     * Generates a new UUID string for database entity IDs.
     *
     * @return UUID string (36 characters)
     */
    public String generateUuid() {
        return UUID.randomUUID().toString();
    }

    /**
     * Gets the current timestamp for database operations.
     *
     * @return Current timestamp
     */
    public LocalDateTime getCurrentTimestamp() {
        return LocalDateTime.now();
    }

    /**
     * Validates if a given string is a valid UUID.
     *
     * @param id String to validate
     * @return true if valid UUID, false otherwise
     */
    public boolean isValidUuid(String id) {
        if (id == null || id.isEmpty()) {
            return false;
        }

        try {
            UUID.fromString(id);
            return true;
        } catch (IllegalArgumentException e) {
            log.debug("Invalid UUID format: {}", id);
            return false;
        }
    }

    /**
     * Sanitizes a string for database insertion.
     * Prevents SQL injection and other security issues.
     *
     * @param input String to sanitize
     * @return Sanitized string
     */
    public String sanitizeString(String input) {
        if (input == null) {
            return null;
        }

        // Basic sanitization - replace special characters
        return input.replaceAll("[;'\"]", "");
    }
}