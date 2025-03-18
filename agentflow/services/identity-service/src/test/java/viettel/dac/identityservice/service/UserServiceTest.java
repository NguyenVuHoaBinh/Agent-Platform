package viettel.dac.identityservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import viettel.dac.identityservice.exception.*;
import viettel.dac.identityservice.model.Organization;
import viettel.dac.identityservice.model.Project;
import viettel.dac.identityservice.model.Role;
import viettel.dac.identityservice.model.User;
import viettel.dac.identityservice.repository.OrganizationRepository;
import viettel.dac.identityservice.repository.ProjectRepository;
import viettel.dac.identityservice.repository.RoleRepository;
import viettel.dac.identityservice.repository.UserRepository;
import viettel.dac.identityservice.security.SecurityUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private Role testRole;
    private Organization testOrganization;
    private Project testProject;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id("1")
                .username("testuser")
                .email("test@example.com")
                .passwordHash("password")
                .firstName("Test")
                .lastName("User")
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .roles(new HashSet<>())
                .organizations(new HashSet<>())
                .projects(new HashSet<>())
                .build();

        testRole = Role.builder()
                .id("1")
                .name("ROLE_USER")
                .description("Regular user role")
                .build();

        testOrganization = Organization.builder()
                .id("1")
                .name("Test Organization")
                .description("Test Organization Description")
                .build();

        testProject = Project.builder()
                .id("1")
                .name("Test Project")
                .description("Test Project Description")
                .organization(testOrganization)
                .build();
    }

    @Test
    void createUser_Success() {
        // Arrange
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        User result = userService.createUser(testUser);

        // Assert
        assertNotNull(result);
        assertEquals(testUser.getId(), result.getId());
        assertEquals(testUser.getUsername(), result.getUsername());
        verify(passwordEncoder).encode(testUser.getPasswordHash());
        verify(userRepository).save(testUser);
    }

    @Test
    void createUser_UsernameExists_ThrowsException() {
        // Arrange
        when(userRepository.existsByUsername(anyString())).thenReturn(true);

        // Act & Assert
        assertThrows(UsernameAlreadyExistsException.class, () -> {
            userService.createUser(testUser);
        });
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void createUser_EmailExists_ThrowsException() {
        // Arrange
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        // Act & Assert
        assertThrows(EmailAlreadyExistsException.class, () -> {
            userService.createUser(testUser);
        });
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void getUserById_UserExists_ReturnsUser() {
        // Arrange
        when(userRepository.findById(anyString())).thenReturn(Optional.of(testUser));

        // Act
        Optional<User> result = userService.getUserById("1");

        // Assert
        assertTrue(result.isPresent());
        assertEquals(testUser.getId(), result.get().getId());
    }

    @Test
    void getUserById_UserDoesNotExist_ReturnsEmpty() {
        // Arrange
        when(userRepository.findById(anyString())).thenReturn(Optional.empty());

        // Act
        Optional<User> result = userService.getUserById("1");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void getUserByUsername_UserExists_ReturnsUser() {
        // Arrange
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(testUser));

        // Act
        Optional<User> result = userService.getUserByUsername("testuser");

        // Assert
        assertTrue(result.isPresent());
        assertEquals(testUser.getUsername(), result.get().getUsername());
    }

    @Test
    void getAllUsers_ReturnsPageOfUsers() {
        // Arrange
        List<User> users = Collections.singletonList(testUser);
        Page<User> userPage = new PageImpl<>(users);
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findAll(pageable)).thenReturn(userPage);

        // Act
        Page<User> result = userService.getAllUsers(pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(testUser.getId(), result.getContent().get(0).getId());
    }

    @Test
    void searchUsers_ReturnsMatchingUsers() {
        // Arrange
        List<User> users = Collections.singletonList(testUser);
        Page<User> userPage = new PageImpl<>(users);
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findBySearchTerm(anyString(), eq(pageable))).thenReturn(userPage);

        // Act
        Page<User> result = userService.searchUsers("test", pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(testUser.getId(), result.getContent().get(0).getId());
    }

    @Test
    void updateUser_UserExists_UpdatesUser() {
        // Arrange
        User updatedUser = User.builder()
                .id("1")
                .username("updateduser")
                .email("updated@example.com")
                .firstName("Updated")
                .lastName("User")
                .enabled(true)
                .build();

        when(userRepository.findById(anyString())).thenReturn(Optional.of(testUser));
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);

        // Act
        User result = userService.updateUser("1", updatedUser);

        // Assert
        assertNotNull(result);
        assertEquals(updatedUser.getUsername(), result.getUsername());
        assertEquals(updatedUser.getEmail(), result.getEmail());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateUser_UserDoesNotExist_ThrowsException() {
        // Arrange
        when(userRepository.findById(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            userService.updateUser("1", testUser);
        });
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUser_UsernameAlreadyExists_ThrowsException() {
        // Arrange
        User updatedUser = User.builder()
                .id("1")
                .username("existinguser")
                .email("test@example.com")
                .build();

        when(userRepository.findById(anyString())).thenReturn(Optional.of(testUser));
        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        // Act & Assert
        assertThrows(UsernameAlreadyExistsException.class, () -> {
            userService.updateUser("1", updatedUser);
        });
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUser_EmailAlreadyExists_ThrowsException() {
        // Arrange
        User updatedUser = User.builder()
                .id("1")
                .username("testuser")
                .email("existing@example.com")
                .build();

        when(userRepository.findById(anyString())).thenReturn(Optional.of(testUser));
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        // Act & Assert
        assertThrows(EmailAlreadyExistsException.class, () -> {
            userService.updateUser("1", updatedUser);
        });
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void deleteUser_UserExists_DeletesUser() {
        // Arrange
        when(userRepository.existsById(anyString())).thenReturn(true);
        doNothing().when(userRepository).deleteById(anyString());

        // Act
        userService.deleteUser("1");

        // Assert
        verify(userRepository).deleteById("1");
    }

    @Test
    void deleteUser_UserDoesNotExist_ThrowsException() {
        // Arrange
        when(userRepository.existsById(anyString())).thenReturn(false);

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            userService.deleteUser("1");
        });
        verify(userRepository, never()).deleteById(anyString());
    }

    @Test
    void addRoleToUser_UserAndRoleExist_AddsRole() {
        // Arrange
        when(userRepository.findById(anyString())).thenReturn(Optional.of(testUser));
        when(roleRepository.findByName(anyString())).thenReturn(Optional.of(testRole));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        User result = userService.addRoleToUser("1", "ROLE_USER");

        // Assert
        assertNotNull(result);
        verify(userRepository).save(testUser);
    }

    @Test
    void addRoleToUser_UserDoesNotExist_ThrowsException() {
        // Arrange
        when(userRepository.findById(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            userService.addRoleToUser("1", "ROLE_USER");
        });
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void addRoleToUser_RoleDoesNotExist_ThrowsException() {
        // Arrange
        when(userRepository.findById(anyString())).thenReturn(Optional.of(testUser));
        when(roleRepository.findByName(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            userService.addRoleToUser("1", "ROLE_NONEXISTENT");
        });
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void getUserRoles_UserExists_ReturnsRoles() {
        // Arrange
        when(roleRepository.findByUserId(anyString())).thenReturn(Collections.singleton(testRole));

        // Act
        Set<Role> result = userService.getUserRoles("1");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testRole.getName(), result.iterator().next().getName());
    }

    @Test
    void changePassword_ValidCurrentPassword_ChangesPassword() {
        // Arrange
        when(userRepository.findById(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(passwordEncoder.encode(anyString())).thenReturn("newEncodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        User result = userService.changePassword("1", "currentPassword", "newPassword");

        // Assert
        assertNotNull(result);
        verify(passwordEncoder).encode("newPassword");
        verify(userRepository).save(testUser);
    }

    @Test
    void changePassword_InvalidCurrentPassword_ThrowsException() {
        // Arrange
        when(userRepository.findById(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            userService.changePassword("1", "wrongPassword", "newPassword");
        });
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void changePassword_UserDoesNotExist_ThrowsException() {
        // Arrange
        when(userRepository.findById(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            userService.changePassword("1", "currentPassword", "newPassword");
        });
    }

    @Test
    void hasRole_UserHasRole_ReturnsTrue() {
        // Arrange
        when(userRepository.hasRole(anyString(), anyString())).thenReturn(true);

        // Act
        boolean result = userService.hasRole("1", "ROLE_USER");

        // Assert
        assertTrue(result);
    }

    @Test
    void hasRole_UserDoesNotHaveRole_ReturnsFalse() {
        // Arrange
        when(userRepository.hasRole(anyString(), anyString())).thenReturn(false);

        // Act
        boolean result = userService.hasRole("1", "ROLE_ADMIN");

        // Assert
        assertFalse(result);
    }
}