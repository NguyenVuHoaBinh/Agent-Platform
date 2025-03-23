package viettel.dac.promptservice.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class SecurityUtilsTest {

    private SecurityUtils securityUtils;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        securityUtils = new SecurityUtils();

        // Set up security context mock
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    public void testGetCurrentUserId_Authenticated() {
        // Set up authenticated user
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("test-user");

        // Get current user ID
        Optional<String> userId = securityUtils.getCurrentUserId();

        // Should return the user ID
        assertTrue(userId.isPresent());
        assertEquals("test-user", userId.get());

        // Verify interactions
        verify(securityContext).getAuthentication();
        verify(authentication).isAuthenticated();
        verify(authentication).getName();
    }

    @Test
    public void testGetCurrentUserId_NotAuthenticated() {
        // Set up non-authenticated user
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(false);

        // Get current user ID
        Optional<String> userId = securityUtils.getCurrentUserId();

        // Should return empty optional
        assertFalse(userId.isPresent());

        // Verify interactions
        verify(securityContext).getAuthentication();
        verify(authentication).isAuthenticated();
        verify(authentication, never()).getName();
    }

    @Test
    public void testGetCurrentUserId_NullAuthentication() {
        // Set up null authentication
        when(securityContext.getAuthentication()).thenReturn(null);

        // Get current user ID
        Optional<String> userId = securityUtils.getCurrentUserId();

        // Should return empty optional
        assertFalse(userId.isPresent());

        // Verify interactions
        verify(securityContext).getAuthentication();
    }

    @Test
    public void testGetCurrentUserId_NullUsername() {
        // Set up authenticated user with null username
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(null);

        // Get current user ID
        Optional<String> userId = securityUtils.getCurrentUserId();

        // Should return empty optional
        assertFalse(userId.isPresent());

        // Verify interactions
        verify(securityContext).getAuthentication();
        verify(authentication).isAuthenticated();
        verify(authentication).getName();
    }
}