package viettel.dac.identityservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import viettel.dac.identityservice.exception.ResourceAlreadyExistsException;
import viettel.dac.identityservice.exception.ResourceNotFoundException;
import viettel.dac.identityservice.exception.UnauthorizedException;
import viettel.dac.identityservice.model.Organization;
import viettel.dac.identityservice.model.User;
import viettel.dac.identityservice.repository.OrganizationRepository;
import viettel.dac.identityservice.repository.UserRepository;
import viettel.dac.identityservice.security.SecurityUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrganizationServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private OrganizationService organizationService;

    private Organization testOrg;
    private User testUser;

    @BeforeEach
    void setUp() {
        testOrg = Organization.builder()
                .id("org123")
                .name("Test Organization")
                .description("Test organization description")
                .users(new HashSet<>())
                .projects(new HashSet<>())
                .build();

        testUser = User.builder()
                .id("user123")
                .username("testuser")
                .email("test@example.com")
                .passwordHash("hashedpassword")
                .firstName("Test")
                .lastName("User")
                .enabled(true)
                .organizations(new HashSet<>())
                .build();
    }

    @Test
    void createOrganization_withValidData_shouldReturnCreatedOrganization() {
        // Arrange
        when(securityUtils.getCurrentUserId()).thenReturn(Optional.of("user123"));
        when(organizationRepository.existsByName(anyString())).thenReturn(false);
        when(userRepository.findById(anyString())).thenReturn(Optional.of(testUser));
        when(organizationRepository.save(any(Organization.class))).thenReturn(testOrg);

        // Act
        Organization result = organizationService.createOrganization(testOrg);

        // Assert
        assertNotNull(result);
        assertEquals(testOrg.getId(), result.getId());
        assertEquals(testOrg.getName(), result.getName());
        assertTrue(result.getUsers().contains(testUser));
        verify(securityUtils).getCurrentUserId();
        verify(organizationRepository).existsByName(testOrg.getName());
        verify(userRepository).findById("user123");
        verify(organizationRepository).save(any(Organization.class));
    }

    @Test
    void createOrganization_withExistingName_shouldThrowResourceAlreadyExistsException() {
        // Arrange
        when(organizationRepository.existsByName(anyString())).thenReturn(true);

        // Act & Assert
        assertThrows(ResourceAlreadyExistsException.class, () -> organizationService.createOrganization(testOrg));
        verify(organizationRepository).existsByName(testOrg.getName());
        verify(organizationRepository, never()).save(any(Organization.class));
    }

    @Test
    void createOrganization_withUnauthenticatedUser_shouldThrowUnauthorizedException() {
        // Arrange
        when(securityUtils.getCurrentUserId()).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UnauthorizedException.class, () -> organizationService.createOrganization(testOrg));
        verify(securityUtils).getCurrentUserId();
        verify(organizationRepository, never()).save(any(Organization.class));
    }

    @Test
    void getOrganizationById_withExistingId_shouldReturnOrganization() {
        // Arrange
        when(organizationRepository.findById(anyString())).thenReturn(Optional.of(testOrg));

        // Act
        Optional<Organization> result = organizationService.getOrganizationById("org123");

        // Assert
        assertTrue(result.isPresent());
        assertEquals(testOrg.getId(), result.get().getId());
        verify(organizationRepository).findById("org123");
    }

    @Test
    void getOrganizationById_withNonExistingId_shouldReturnEmpty() {
        // Arrange
        when(organizationRepository.findById(anyString())).thenReturn(Optional.empty());

        // Act
        Optional<Organization> result = organizationService.getOrganizationById("nonexistent");

        // Assert
        assertFalse(result.isPresent());
        verify(organizationRepository).findById("nonexistent");
    }

    @Test
    void updateOrganization_withValidData_shouldReturnUpdatedOrganization() {
        // Arrange
        Organization updatedOrg = Organization.builder()
                .id("org123")
                .name("Updated Organization")
                .description("Updated description")
                .build();

        when(organizationRepository.findById(anyString())).thenReturn(Optional.of(testOrg));
        when(organizationRepository.existsByName(anyString())).thenReturn(false);
        when(organizationRepository.save(any(Organization.class))).thenReturn(updatedOrg);

        // Act
        Organization result = organizationService.updateOrganization("org123", updatedOrg);

        // Assert
        assertNotNull(result);
        assertEquals("Updated Organization", result.getName());
        assertEquals("Updated description", result.getDescription());
        verify(organizationRepository).findById("org123");
        verify(organizationRepository).existsByName("Updated Organization");
        verify(organizationRepository).save(any(Organization.class));
    }

    @Test
    void updateOrganization_withExistingName_shouldThrowResourceAlreadyExistsException() {
        // Arrange
        Organization updatedOrg = Organization.builder()
                .id("org123")
                .name("Existing Organization")
                .description("Updated description")
                .build();

        when(organizationRepository.findById(anyString())).thenReturn(Optional.of(testOrg));
        when(organizationRepository.existsByName("Existing Organization")).thenReturn(true);

        // Act & Assert
        assertThrows(ResourceAlreadyExistsException.class,
                () -> organizationService.updateOrganization("org123", updatedOrg));
        verify(organizationRepository).findById("org123");
        verify(organizationRepository).existsByName("Existing Organization");
        verify(organizationRepository, never()).save(any(Organization.class));
    }

    @Test
    void deleteOrganization_withExistingId_shouldDeleteOrganization() {
        // Arrange
        when(organizationRepository.existsById(anyString())).thenReturn(true);
        doNothing().when(organizationRepository).deleteById(anyString());

        // Act
        organizationService.deleteOrganization("org123");

        // Assert
        verify(organizationRepository).existsById("org123");
        verify(organizationRepository).deleteById("org123");
    }

    @Test
    void deleteOrganization_withNonExistingId_shouldThrowResourceNotFoundException() {
        // Arrange
        when(organizationRepository.existsById(anyString())).thenReturn(false);

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> organizationService.deleteOrganization("nonexistent"));
        verify(organizationRepository).existsById("nonexistent");
        verify(organizationRepository, never()).deleteById(anyString());
    }

    @Test
    void addUserToOrganization_withValidIds_shouldReturnUpdatedOrganization() {
        // Arrange
        when(organizationRepository.findById(anyString())).thenReturn(Optional.of(testOrg));
        when(userRepository.findById(anyString())).thenReturn(Optional.of(testUser));
        when(organizationRepository.isMember(anyString(), anyString())).thenReturn(false);
        when(organizationRepository.save(any(Organization.class))).thenReturn(testOrg);

        // Act
        Organization result = organizationService.addUserToOrganization("org123", "user123");

        // Assert
        assertNotNull(result);
        assertTrue(result.getUsers().contains(testUser));
        assertTrue(testUser.getOrganizations().contains(testOrg));
        verify(organizationRepository).findById("org123");
        verify(userRepository).findById("user123");
        verify(organizationRepository).isMember("org123", "user123");
        verify(organizationRepository).save(any(Organization.class));
    }

    @Test
    void addUserToOrganization_whenUserAlreadyMember_shouldThrowResourceAlreadyExistsException() {
        // Arrange
        when(organizationRepository.findById(anyString())).thenReturn(Optional.of(testOrg));
        when(userRepository.findById(anyString())).thenReturn(Optional.of(testUser));
        when(organizationRepository.isMember(anyString(), anyString())).thenReturn(true);

        // Act & Assert
        assertThrows(ResourceAlreadyExistsException.class,
                () -> organizationService.addUserToOrganization("org123", "user123"));
        verify(organizationRepository).findById("org123");
        verify(userRepository).findById("user123");
        verify(organizationRepository).isMember("org123", "user123");
        verify(organizationRepository, never()).save(any(Organization.class));
    }

    @Test
    void removeUserFromOrganization_withValidIds_shouldReturnUpdatedOrganization() {
        // Arrange
        testOrg.getUsers().add(testUser);
        testUser.getOrganizations().add(testOrg);

        when(organizationRepository.findById(anyString())).thenReturn(Optional.of(testOrg));
        when(userRepository.findById(anyString())).thenReturn(Optional.of(testUser));
        when(organizationRepository.isMember(anyString(), anyString())).thenReturn(true);
        when(organizationRepository.save(any(Organization.class))).thenReturn(testOrg);

        // Add another user to ensure we don't remove the last user
        User anotherUser = User.builder().id("user456").build();
        testOrg.getUsers().add(anotherUser);

        // Act
        Organization result = organizationService.removeUserFromOrganization("org123", "user123");

        // Assert
        assertNotNull(result);
        assertFalse(result.getUsers().contains(testUser));
        assertFalse(testUser.getOrganizations().contains(testOrg));
        verify(organizationRepository).findById("org123");
        verify(userRepository).findById("user123");
        verify(organizationRepository).isMember("org123", "user123");
        verify(organizationRepository).save(any(Organization.class));
    }

    @Test
    void removeUserFromOrganization_whenUserNotMember_shouldThrowResourceNotFoundException() {
        // Arrange
        when(organizationRepository.findById(anyString())).thenReturn(Optional.of(testOrg));
        when(userRepository.findById(anyString())).thenReturn(Optional.of(testUser));
        when(organizationRepository.isMember(anyString(), anyString())).thenReturn(false);

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> organizationService.removeUserFromOrganization("org123", "user123"));
        verify(organizationRepository).findById("org123");
        verify(userRepository).findById("user123");
        verify(organizationRepository).isMember("org123", "user123");
        verify(organizationRepository, never()).save(any(Organization.class));
    }

    @Test
    void removeUserFromOrganization_whenLastUser_shouldThrowIllegalStateException() {
        // Arrange
        testOrg.getUsers().add(testUser);
        testUser.getOrganizations().add(testOrg);

        when(organizationRepository.findById(anyString())).thenReturn(Optional.of(testOrg));
        when(userRepository.findById(anyString())).thenReturn(Optional.of(testUser));
        when(organizationRepository.isMember(anyString(), anyString())).thenReturn(true);

        // Act & Assert
        assertThrows(IllegalStateException.class,
                () -> organizationService.removeUserFromOrganization("org123", "user123"));
        verify(organizationRepository).findById("org123");
        verify(userRepository).findById("user123");
        verify(organizationRepository).isMember("org123", "user123");
        verify(organizationRepository, never()).save(any(Organization.class));
    }

    @Test
    void getOrganizationMembers_withExistingId_shouldReturnMembers() {
        // Arrange
        List<User> members = Arrays.asList(testUser);
        when(organizationRepository.existsById(anyString())).thenReturn(true);
        when(userRepository.findByOrganizationId(anyString())).thenReturn(members);

        // Act
        List<User> result = organizationService.getOrganizationMembers("org123");

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testUser.getId(), result.get(0).getId());
        verify(organizationRepository).existsById("org123");
        verify(userRepository).findByOrganizationId("org123");
    }

    @Test
    void getOrganizationMembers_withNonExistingId_shouldThrowResourceNotFoundException() {
        // Arrange
        when(organizationRepository.existsById(anyString())).thenReturn(false);

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> organizationService.getOrganizationMembers("nonexistent"));
        verify(organizationRepository).existsById("nonexistent");
        verify(userRepository, never()).findByOrganizationId(anyString());
    }
}