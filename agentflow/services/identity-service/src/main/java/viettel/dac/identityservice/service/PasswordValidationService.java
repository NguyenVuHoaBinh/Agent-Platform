package viettel.dac.identityservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Service for validating password strength
 */
@Service
public class PasswordValidationService {

    @Value("${security.password.min-length:8}")
    private int minLength;

    @Value("${security.password.require-uppercase:true}")
    private boolean requireUppercase;

    @Value("${security.password.require-lowercase:true}")
    private boolean requireLowercase;

    @Value("${security.password.require-digit:true}")
    private boolean requireDigit;

    @Value("${security.password.require-special-char:true}")
    private boolean requireSpecialChar;

    // Common password list (abbreviated - would be longer in production)
    private static final List<String> COMMON_PASSWORDS = List.of(
            "password", "123456", "qwerty", "admin", "welcome",
            "123456789", "12345678", "abc123", "password1", "admin123"
    );

    /**
     * Validate password strength
     *
     * @param password Password to validate
     * @return List of validation errors, empty if valid
     */
    public List<String> validatePassword(String password) {
        List<String> errors = new ArrayList<>();

        // Check for null or empty
        if (password == null || password.trim().isEmpty()) {
            errors.add("Password cannot be empty");
            return errors;
        }

        // Check minimum length
        if (password.length() < minLength) {
            errors.add("Password must be at least " + minLength + " characters long");
        }

        // Check for uppercase letters
        if (requireUppercase && !Pattern.compile("[A-Z]").matcher(password).find()) {
            errors.add("Password must contain at least one uppercase letter");
        }

        // Check for lowercase letters
        if (requireLowercase && !Pattern.compile("[a-z]").matcher(password).find()) {
            errors.add("Password must contain at least one lowercase letter");
        }

        // Check for digits
        if (requireDigit && !Pattern.compile("\\d").matcher(password).find()) {
            errors.add("Password must contain at least one digit");
        }

        // Check for special characters
        if (requireSpecialChar && !Pattern.compile("[^a-zA-Z0-9]").matcher(password).find()) {
            errors.add("Password must contain at least one special character");
        }

        // Check against common passwords
        if (COMMON_PASSWORDS.contains(password.toLowerCase())) {
            errors.add("Password is too common and easily guessable");
        }

        return errors;
    }

    /**
     * Check if a password is valid
     *
     * @param password Password to validate
     * @return True if password is valid
     */
    public boolean isValid(String password) {
        return validatePassword(password).isEmpty();
    }
}