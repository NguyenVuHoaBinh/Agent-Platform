package viettel.dac.identityservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import viettel.dac.identityservice.dto.PasswordChangeRequest;
import viettel.dac.identityservice.exception.ResourceNotFoundException;
import viettel.dac.identityservice.exception.UsernameAlreadyExistsException;
import viettel.dac.identityservice.model.Organization;
import viettel.dac.identityservice.model.Project;
import viettel.dac.identityservice.model.Role;
import viettel.dac.identityservice.model.User;
import viettel.dac.identityservice.security.SecurityUtils;
import viettel.dac.identityservice.service.UserService;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private SecurityUtils securityUtils;

    private User testUser;
    private Role userRole;
    private Organization testOrg;
    private Project testProject;

    @BeforeEach
    void setUp() {
        userRole = Role.builder()
                .id("role123")
                .name("ROLE_USER")
                .description("Regular user role")
                .permissions(new HashSet<>())
                .build();

        Set<Role> roles = new HashSet<>();
        roles.add(userRole);

        testUser = User.builder()
                .id("user123")
                .username("testuser")
                .email("test@example.com")
                .passwordHash("hashedpassword")
                .firstName("Test")
                .lastName("User")
                .enabled(true)
                .roles(roles)
                .build();

        testOrg = Organization.builder()
                .id("org123")
                .name("Test Organization")
                .description("Test organization description")
                .build();

        testProject = Project.builder()
                .id("project123")
                .name("Test Project")
                .description("Test project description")
                .organization(testOrg)
                .build();
    }

    @Test
    @WithMockUser(authorities = "READ_USERS")
    void getAllUsers_shouldReturnUsersList() throws Exception {
        // Arrange
        List<User> users = Collections.singletonList(testUser);
        when(userService.getAllUsers(any(Pageable.class))).thenReturn(new PageImpl<>(users));

        // Act & Assert
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("user123"))
                .andExpect(jsonPath("$.content[0].username").value("testuser"))
                .andExpect(jsonPath("$.content[0].email").value("test@example.com"))
                .andExpect(jsonPath("$.content[0].roles[0]").value("ROLE_USER"));
    }

    @Test
    @WithMockUser(authorities = "READ_USERS")
    void searchUsers_shouldReturnMatchingUsers() throws Exception {
        // Arrange
        List<User> users = Collections.singletonList(testUser);
        when(userService.searchUsers(anyString(), any(Pageable.class))).thenReturn(new PageImpl<>(users));

        // Act & Assert
        mockMvc.perform(get("/api/users/search")
                        .param("query", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("user123"))
                .andExpect(jsonPath("$.content[0].username").value("testuser"));
    }

    @Test
    @WithMockUser(authorities = "READ_USERS")
    void getUserById_withExistingId_shouldReturnUser() throws Exception {
        // Arrange
        when(userService.getUserById(anyString())).thenReturn(Optional.of(testUser));

        // Act & Assert
        mockMvc.perform(get("/api/users/user123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("user123"))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    @WithMockUser(authorities = "READ_USERS")
    void getUserById_withNonExistingId_shouldReturnNotFound() throws Exception {
        // Arrange
        when(userService.getUserById(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/api/users/nonexistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "WRITE_USERS")
    void createUser_withValidData_shouldReturnCreatedUser() throws Exception {
        // Arrange
        when(userService.createUser(any(User.class))).thenReturn(testUser);

        // Act & Assert
        mockMvc.perform(post("/api/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testUser)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("user123"))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    @WithMockUser(authorities = "WRITE_USERS")
    void createUser_withExistingUsername_shouldReturnConflict() throws Exception {
        // Arrange
        when(userService.createUser(any(User.class)))
                .thenThrow(new UsernameAlreadyExistsException("Username already in use"));

        // Act & Assert
        mockMvc.perform(post("/api/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testUser)))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(authorities = "WRITE_USERS")
    void updateUser_withValidData_shouldReturnUpdatedUser() throws Exception {
        // Arrange
        when(userService.updateUser(anyString(), any(User.class))).thenReturn(testUser);

        // Act & Assert
        mockMvc.perform(put("/api/users/user123")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("user123"))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    @WithMockUser(authorities = "WRITE_USERS")
    void updateUser_withNonExistingId_shouldReturnNotFound() throws Exception {
        // Arrange
        when(userService.updateUser(anyString(), any(User.class)))
                .thenThrow(new ResourceNotFoundException("User not found"));

        // Act & Assert
        mockMvc.perform(put("/api/users/nonexistent")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testUser)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(authorities = "WRITE_USERS")
    void deleteUser_withExistingId_shouldReturnSuccess() throws Exception {
        // Arrange
        doNothing().when(userService).deleteUser(anyString());

        // Act & Assert
        mockMvc.perform(delete("/api/users/user123")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User deleted successfully"));
    }

    @Test
    @WithMockUser(authorities = "WRITE_USERS")
    void deleteUser_withNonExistingId_shouldReturnNotFound() throws Exception {
        // Arrange
        doThrow(new ResourceNotFoundException("User not found"))
                .when(userService).deleteUser(anyString());

        // Act & Assert
        mockMvc.perform(delete("/api/users/nonexistent")
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "testuser")
    void changePassword_withValidRequest_shouldReturnSuccess() throws Exception {
        // Arrange
        PasswordChangeRequest request = new PasswordChangeRequest(
                "currentPassword", "newPassword", "newPassword");
        when(securityUtils.isCurrentUser(anyString())).thenReturn(true);
        when(userService.changePassword(anyString(), anyString(), anyString())).thenReturn(testUser);

        // Act & Assert
        mockMvc.perform(put("/api/users/user123/password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Password changed successfully"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void changePassword_withPasswordMismatch_shouldReturnBadRequest() throws Exception {
        // Arrange
        PasswordChangeRequest request = new PasswordChangeRequest(
                "currentPassword", "newPassword", "differentPassword");
        when(securityUtils.isCurrentUser(anyString())).thenReturn(true);

        // Act & Assert
        mockMvc.perform(put("/api/users/user123/password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Password confirmation does not match"));
    }

    @Test
    @WithMockUser(authorities = "READ_USERS")
    void getUserRoles_withExistingId_shouldReturnRoles() throws Exception {
        // Arrange
        when(userService.getUserRoles(anyString())).thenReturn(Collections.singleton(userRole));

        // Act & Assert
        mockMvc.perform(get("/api/users/user123/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("role123"))
                .andExpect(jsonPath("$[0].name").value("ROLE_USER"))
                .andExpect(jsonPath("$[0].description").value("Regular user role"));
    }

    @Test
    @WithMockUser(authorities = "WRITE_USERS")
    void addRoleToUser_withValidData_shouldReturnUpdatedUser() throws Exception {
        // Arrange
        when(userService.addRoleToUser(anyString(), anyString())).thenReturn(testUser);

        // Act & Assert
        mockMvc.perform(put("/api/users/user123/roles/ROLE_ADMIN")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("user123"))
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    @WithMockUser(authorities = "READ_USERS")
    void getUserOrganizations_withExistingId_shouldReturnOrganizations() throws Exception {
        // Arrange
        when(userService.getUserOrganizations(anyString())).thenReturn(Collections.singletonList(testOrg));

        // Act & Assert
        mockMvc.perform(get("/api/users/user123/organizations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("org123"))
                .andExpect(jsonPath("$[0].name").value("Test Organization"))
                .andExpect(jsonPath("$[0].description").value("Test organization description"));
    }

    @Test
    @WithMockUser(authorities = "READ_USERS")
    void getUserProjects_withExistingId_shouldReturnProjects() throws Exception {
        // Arrange
        when(userService.getUserProjects(anyString())).thenReturn(Collections.singletonList(testProject));

        // Act & Assert
        mockMvc.perform(get("/api/users/user123/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("project123"))
                .andExpect(jsonPath("$[0].name").value("Test Project"))
                .andExpect(jsonPath("$[0].description").value("Test project description"))
                .andExpect(jsonPath("$[0].organizationId").value("org123"))
                .andExpect(jsonPath("$[0].organizationName").value("Test Organization"));
    }
}