package viettel.dac.identityservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import viettel.dac.identityservice.dto.JwtAuthenticationResponse;
import viettel.dac.identityservice.dto.LoginRequest;
import viettel.dac.identityservice.dto.SignUpRequest;
import viettel.dac.identityservice.exception.EmailAlreadyExistsException;
import viettel.dac.identityservice.exception.ResourceNotFoundException;
import viettel.dac.identityservice.exception.UsernameAlreadyExistsException;
import viettel.dac.identityservice.model.Role;
import viettel.dac.identityservice.model.User;
import viettel.dac.identityservice.repository.RoleRepository;
import viettel.dac.identityservice.repository.UserRepository;
import viettel.dac.identityservice.security.JwtTokenProvider;
import viettel.dac.identityservice.security.UserDetailsImpl;

import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private AuthService authService;

    private LoginRequest loginRequest;
    private SignUpRequest signUpRequest;
    private User testUser;
    private Role testRole;
    private UserDetailsImpl userDetails;

    @BeforeEach
    void setUp() {
        loginRequest = new LoginRequest("testuser", "password");

        signUpRequest = new SignUpRequest();
        signUpRequest.setUsername("newuser");
        signUpRequest.setEmail("newuser@example.com");
        signUpRequest.setPassword("password");
        signUpRequest.setFirstName("New");
        signUpRequest.setLastName("User");

        testUser = User.builder()
                .id("1")
                .username("testuser")
                .email("test@example.com")
                .passwordHash("encodedPassword")
                .firstName("Test")
                .lastName("User")
                .enabled(true)
                .roles(new HashSet<>())
                .build();

        testRole = Role.builder()
                .id("1")
                .name("ROLE_USER")
                .description("Regular user role")
                .build();

        userDetails = UserDetailsImpl.builder()
                .id("1")
                .username("testuser")
                .email("test@example.com")
                .build();
    }

    @Test
    void login_ValidCredentials_ReturnsJwtToken() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(tokenProvider.generateToken(authentication)).thenReturn("jwtToken");

        // Act
        JwtAuthenticationResponse response = authService.login(loginRequest);

        // Assert
        assertNotNull(response);
        assertEquals("jwtToken", response.getToken());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(tokenProvider).generateToken(authentication);
    }

    @Test
    void login_InvalidCredentials_ThrowsException() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        // Act & Assert
        assertThrows(BadCredentialsException.class, () -> {
            authService.login(loginRequest);
        });
    }

    @Test
    void register_ValidRequest_CreatesUser() {
        // Arrange
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(testRole));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        User result = authService.register(signUpRequest);

        // Assert
        assertNotNull(result);
        verify(passwordEncoder).encode(signUpRequest.getPassword());
        verify(roleRepository).findByName("ROLE_USER");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_UsernameExists_ThrowsException() {
        // Arrange
        when(userRepository.existsByUsername(anyString())).thenReturn(true);

        // Act & Assert
        assertThrows(UsernameAlreadyExistsException.class, () -> {
            authService.register(signUpRequest);
        });
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_EmailExists_ThrowsException() {
        // Arrange
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        // Act & Assert
        assertThrows(EmailAlreadyExistsException.class, () -> {
            authService.register(signUpRequest);
        });
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_RoleNotFound_ThrowsException() {
        // Arrange
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            authService.register(signUpRequest);
        });
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void getCurrentUser_Authenticated_ReturnsUserDetails() {
        // Arrange
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        SecurityContextHolder.setContext(securityContext);

        // Act
        UserDetailsImpl result = authService.getCurrentUser();

        // Assert
        assertNotNull(result);
        assertEquals(userDetails.getId(), result.getId());
        assertEquals(userDetails.getUsername(), result.getUsername());

        // Clean up
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentUser_NotAuthenticated_ReturnsNull() {
        // Arrange
        when(securityContext.getAuthentication()).thenReturn(null);
        SecurityContextHolder.setContext(securityContext);

        // Act
        UserDetailsImpl result = authService.getCurrentUser();

        // Assert
        assertNull(result);

        // Clean up
        SecurityContextHolder.clearContext();
    }

    @Test
    void validateToken_ValidToken_ReturnsTrue() {
        // Arrange
        when(tokenProvider.validateToken(anyString())).thenReturn(true);

        // Act
        boolean result = authService.validateToken("validToken");

        // Assert
        assertTrue(result);
    }

    @Test
    void validateToken_InvalidToken_ReturnsFalse() {
        // Arrange
        when(tokenProvider.validateToken(anyString())).thenReturn(false);

        // Act
        boolean result = authService.validateToken("invalidToken");

        // Assert
        assertFalse(result);
    }

    @Test
    void getUserIdFromToken_ValidToken_ReturnsUserId() {
        // Arrange
        when(tokenProvider.getUserIdFromToken(anyString())).thenReturn("1");

        // Act
        String result = authService.getUserIdFromToken("validToken");

        // Assert
        assertEquals("1", result);
    }

    @Test
    void initiatePasswordReset_UserExists_ReturnsTrue() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));

        // Act
        boolean result = authService.initiatePasswordReset("test@example.com");

        // Assert
        assertTrue(result);
    }

    @Test
    void initiatePasswordReset_UserDoesNotExist_ReturnsTrueForSecurity() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // Act
        boolean result = authService.initiatePasswordReset("nonexistent@example.com");

        // Assert
        // We still return true even if the user doesn't exist for security reasons
        assertTrue(result);
    }
}