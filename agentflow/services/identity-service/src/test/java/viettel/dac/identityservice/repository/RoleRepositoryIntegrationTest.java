package viettel.dac.identityservice.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import viettel.dac.identityservice.model.Permission;
import viettel.dac.identityservice.model.Role;

import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
public class RoleRepositoryIntegrationTest {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Test
    void findByName_withExistingName_shouldReturnRole() {
        // Arrange
        Role role = Role.builder()
                .name("ROLE_TEST")
                .description("Test role")
                .permissions(new HashSet<>())
                .build();
        roleRepository.save(role);

        // Act
        Optional<Role> result = roleRepository.findByName("ROLE_TEST");

        // Assert
        assertTrue(result.isPresent());
        assertEquals("ROLE_TEST", result.get().getName());
        assertEquals("Test role", result.get().getDescription());
    }

    @Test
    void findByName_withNonExistingName_shouldReturnEmpty() {
        // Act
        Optional<Role> result = roleRepository.findByName("NONEXISTENT_ROLE");

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    void existsByName_withExistingName_shouldReturnTrue() {
        // Arrange
        Role role = Role.builder()
                .name("ROLE_TEST")
                .description("Test role")
                .permissions(new HashSet<>())
                .build();
        roleRepository.save(role);

        // Act
        boolean result = roleRepository.existsByName("ROLE_TEST");

        // Assert
        assertTrue(result);
    }

    @Test
    void existsByName_withNonExistingName_shouldReturnFalse() {
        // Act
        boolean result = roleRepository.existsByName("NONEXISTENT_ROLE");

        // Assert
        assertFalse(result);
    }

    @Test
    void findByIdWithPermissions_withExistingId_shouldReturnRoleWithPermissions() {
        // Arrange
        Role role = Role.builder()
                .name("ROLE_TEST")
                .description("Test role")
                .permissions(new HashSet<>())
                .build();
        Role savedRole = roleRepository.save(role);

        Permission permission = Permission.builder()
                .name("TEST_PERMISSION")
                .description("Test permission")
                .roles(new HashSet<>())
                .build();
        Permission savedPermission = permissionRepository.save(permission);

        savedRole.getPermissions().add(savedPermission);
        savedPermission.getRoles().add(savedRole);
        roleRepository.save(savedRole);

        // Act
        Optional<Role> result = roleRepository.findByIdWithPermissions(savedRole.getId());

        // Assert
        assertTrue(result.isPresent());
        assertEquals("ROLE_TEST", result.get().getName());
        assertEquals(1, result.get().getPermissions().size());
        assertTrue(result.get().getPermissions().stream()
                .anyMatch(p -> p.getName().equals("TEST_PERMISSION")));
    }
}