package viettel.dac.identityservice.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import viettel.dac.identityservice.model.Organization;
import viettel.dac.identityservice.model.User;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
public class OrganizationRepositoryIntegrationTest {

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByName_withExistingName_shouldReturnOrganization() {
        // Arrange
        Organization organization = Organization.builder()
                .name("Test Organization")
                .description("Test organization description")
                .users(new HashSet<>())
                .projects(new HashSet<>())
                .build();
        organizationRepository.save(organization);

        // Act
        Optional<Organization> result = organizationRepository.findByName("Test Organization");

        // Assert
        assertTrue(result.isPresent());
        assertEquals("Test Organization", result.get().getName());
        assertEquals("Test organization description", result.get().getDescription());
    }

    @Test
    void existsByName_withExistingName_shouldReturnTrue() {
        // Arrange
        Organization organization = Organization.builder()
                .name("Test Organization")
                .description("Test organization description")
                .users(new HashSet<>())
                .projects(new HashSet<>())
                .build();
        organizationRepository.save(organization);

        // Act
        boolean result = organizationRepository.existsByName("Test Organization");

        // Assert
        assertTrue(result);
    }

    @Test
    void findBySearchTerm_withMatchingName_shouldReturnOrganization() {
        // Arrange
        Organization organization1 = Organization.builder()
                .name("Test Organization")
                .description("First organization")
                .users(new HashSet<>())
                .projects(new HashSet<>())
                .build();
        Organization organization2 = Organization.builder()
                .name("Another Organization")
                .description("Second organization")
                .users(new HashSet<>())
                .projects(new HashSet<>())
                .build();
        organizationRepository.save(organization1);
        organizationRepository.save(organization2);

        // Act
        Page<Organization> result = organizationRepository.findBySearchTerm("Test", PageRequest.of(0, 10));

        // Assert
        assertEquals(1, result.getTotalElements());
        assertEquals("Test Organization", result.getContent().get(0).getName());
    }

    @Test
    void findByUserId_withUserInOrganization_shouldReturnOrganization() {
        // Arrange
        User user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .passwordHash("hashedpassword")
                .firstName("Test")
                .lastName("User")
                .enabled(true)
                .organizations(new HashSet<>())
                .build();
        User savedUser = userRepository.save(user);

        Organization organization = Organization.builder()
                .name("Test Organization")
                .description("Test organization description")
                .users(new HashSet<>())
                .projects(new HashSet<>())
                .build();
        Organization savedOrg = organizationRepository.save(organization);

        savedUser.getOrganizations().add(savedOrg);
        savedOrg.getUsers().add(savedUser);
        userRepository.save(savedUser);

        // Act
        List<Organization> result = organizationRepository.findByUserId(savedUser.getId());

        // Assert
        assertEquals(1, result.size());
        assertEquals("Test Organization", result.get(0).getName());
    }

    @Test
    void isMember_withUserInOrganization_shouldReturnTrue() {
        // Arrange
        User user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .passwordHash("hashedpassword")
                .firstName("Test")
                .lastName("User")
                .enabled(true)
                .organizations(new HashSet<>())
                .build();
        User savedUser = userRepository.save(user);

        Organization organization = Organization.builder()
                .name("Test Organization")
                .description("Test organization description")
                .users(new HashSet<>())
                .projects(new HashSet<>())
                .build();
        Organization savedOrg = organizationRepository.save(organization);

        savedUser.getOrganizations().add(savedOrg);
        savedOrg.getUsers().add(savedUser);
        userRepository.save(savedUser);

        // Act
        boolean result = organizationRepository.isMember(savedOrg.getId(), savedUser.getId());

        // Assert
        assertTrue(result);
    }

    @Test
    void isMember_withUserNotInOrganization_shouldReturnFalse() {
        // Arrange
        User user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .passwordHash("hashedpassword")
                .firstName("Test")
                .lastName("User")
                .enabled(true)
                .organizations(new HashSet<>())
                .build();
        User savedUser = userRepository.save(user);

        Organization organization = Organization.builder()
                .name("Test Organization")
                .description("Test organization description")
                .users(new HashSet<>())
                .projects(new HashSet<>())
                .build();
        Organization savedOrg = organizationRepository.save(organization);

        // Act
        boolean result = organizationRepository.isMember(savedOrg.getId(), savedUser.getId());

        // Assert
        assertFalse(result);
    }
}