package viettel.dac.identityservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import viettel.dac.identityservice.dto.LoginRequest;
import viettel.dac.identityservice.dto.JwtAuthenticationResponse;
import viettel.dac.identityservice.dto.SignUpRequest;
import viettel.dac.identityservice.exception.EmailAlreadyExistsException;
import viettel.dac.identityservice.exception.UsernameAlreadyExistsException;
import viettel.dac.identityservice.model.User;
import viettel.dac.identityservice.security.UserDetailsImpl;
import viettel.dac.identityservice.service.AuthService;
import viettel.dac.identityservice.service.UserService;

import java.util.HashSet;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private UserService userService;

    private LoginRequest loginRequest;
    private SignUpRequest signUpRequest;
    private User testUser;

    @BeforeEach
    void setUp() {
        loginRequest = new LoginRequest("testuser", "password123");

        signUpRequest = new SignUpRequest();
        signUpRequest.setUsername("newuser");
        signUpRequest.setEmail("new@example.com");
        signUpRequest.setPassword("password123");
        signUpRequest.setFirstName("New");
        signUpRequest.setLastName("User");

        testUser = User.builder()
                .id("user123")
                .username("testuser")
                .email("test@example.com")
                .passwordHash("hashedpassword")
                .firstName("Test")
                .lastName("User")
                .enabled(true)
                .roles(new HashSet<>())
                .build();
    }

    @Test
    void login_withValidCredentials_shouldReturnToken() throws Exception {
        // Arrange
        JwtAuthenticationResponse response = new JwtAuthenticationResponse("jwt-token");
        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void register_withValidData_shouldReturnSuccess() throws Exception {
        // Arrange
        when(authService.register(any(SignUpRequest.class))).thenReturn(testUser);

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signUpRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User registered successfully"))
                .andExpect(jsonPath("$.data.id").value("user123"))
                .andExpect(jsonPath("$.data.username").value("testuser"));
    }

    @Test
    void register_withExistingUsername_shouldReturnConflict() throws Exception {
        // Arrange
        when(authService.register(any(SignUpRequest.class)))
                .thenThrow(new UsernameAlreadyExistsException("Username is already taken"));

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signUpRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Username is already taken"));
    }

    @Test
    void register_withExistingEmail_shouldReturnConflict() throws Exception {
        // Arrange
        when(authService.register(any(SignUpRequest.class)))
                .thenThrow(new EmailAlreadyExistsException("Email is already in use"));

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signUpRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Email is already in use"));
    }

    @Test
    @WithMockUser
    void getCurrentUser_whenAuthenticated_shouldReturnUserDetails() throws Exception {
        // Arrange
        UserDetailsImpl userDetails = mock(UserDetailsImpl.class);
        when(userDetails.getId()).thenReturn("user123");
        when(authService.getCurrentUser()).thenReturn(userDetails);
        when(userService.getUserById(anyString())).thenReturn(Optional.of(testUser));

        // Act & Assert
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("user123"))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void getCurrentUser_whenNotAuthenticated_shouldReturnUnauthorized() throws Exception {
        // Arrange
        when(authService.getCurrentUser()).thenReturn(null);

        // Act & Assert
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validateToken_withValidToken_shouldReturnSuccess() throws Exception {
        // Arrange
        when(authService.validateToken(anyString())).thenReturn(true);

        // Act & Assert
        mockMvc.perform(post("/api/auth/validate-token")
                        .with(csrf())
                        .param("token", "valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Token is valid"));
    }

    @Test
    void validateToken_withInvalidToken_shouldReturnUnauthorized() throws Exception {
        // Arrange
        when(authService.validateToken(anyString())).thenReturn(false);

        // Act & Assert
        mockMvc.perform(post("/api/auth/validate-token")
                        .with(csrf())
                        .param("token", "invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid or expired token"));
    }
}