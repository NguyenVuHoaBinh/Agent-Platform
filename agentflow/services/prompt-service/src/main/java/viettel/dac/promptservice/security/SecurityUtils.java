package viettel.dac.promptservice.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Utility class for security operations
 */
@Component
@RequiredArgsConstructor
public class SecurityUtils {

    /**
     * Get the current authenticated user ID
     *
     * @return Optional containing user ID if authenticated, empty otherwise
     */
    public Optional<String> getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        return Optional.ofNullable(authentication.getName());
    }
}