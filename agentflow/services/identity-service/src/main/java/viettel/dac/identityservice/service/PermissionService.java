package viettel.dac.identityservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import viettel.dac.identityservice.exception.ResourceAlreadyExistsException;
import viettel.dac.identityservice.exception.ResourceNotFoundException;
import viettel.dac.identityservice.model.Permission;
import viettel.dac.identityservice.model.Role;
import viettel.dac.identityservice.repository.PermissionRepository;
import viettel.dac.identityservice.repository.RoleRepository;
import viettel.dac.identityservice.repository.UserRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class PermissionService {
    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    /**
     * Create a new permission
     *
     * @param permission The permission to create
     * @return The created permission
     */
    public Permission createPermission(Permission permission) {
        // Check if permission name already exists
        if (permissionRepository.existsByName(permission.getName())) {
            throw new ResourceAlreadyExistsException("Permission name already exists: " + permission.getName());
        }

        // Generate ID if not provided
        if (permission.getId() == null) {
            permission.setId(UUID.randomUUID().toString());
        }

        log.debug("Creating new permission: {}", permission.getName());
        return permissionRepository.save(permission);
    }

    /**
     * Get a permission by ID
     *
     * @param id The permission ID
     * @return The permission if found
     */
    public Optional<Permission> getPermissionById(String id) {
        return permissionRepository.findById(id);
    }

    /**
     * Get a permission by name
     *
     * @param name The permission name
     * @return The permission if found
     */
    public Optional<Permission> getPermissionByName(String name) {
        return permissionRepository.findByName(name);
    }

    /**
     * Get all permissions
     *
     * @return List of all permissions
     */
    public List<Permission> getAllPermissions() {
        return permissionRepository.findAll();
    }

    /**
     * Update a permission
     *
     * @param id The permission ID
     * @param updatedPermission The updated permission data
     * @return The updated permission
     */
    public Permission updatePermission(String id, Permission updatedPermission) {
        Permission existingPermission = permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found with id: " + id));

        // Check if name is changed and already exists
        if (!existingPermission.getName().equals(updatedPermission.getName()) &&
                permissionRepository.existsByName(updatedPermission.getName())) {
            throw new ResourceAlreadyExistsException("Permission name already exists: " + updatedPermission.getName());
        }

        // Update fields
        existingPermission.setName(updatedPermission.getName());
        existingPermission.setDescription(updatedPermission.getDescription());

        log.debug("Updating permission: {}", existingPermission.getName());
        return permissionRepository.save(existingPermission);
    }

    /**
     * Delete a permission
     *
     * @param id The permission ID
     */
    public void deletePermission(String id) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found with id: " + id));

        // Check if permission is assigned to any roles
        if (!permission.getRoles().isEmpty()) {
            throw new IllegalStateException("Cannot delete permission that is assigned to roles");
        }

        log.debug("Deleting permission: {}", permission.getName());
        permissionRepository.delete(permission);
    }

    /**
     * Get permissions by role ID
     *
     * @param roleId The role ID
     * @return Set of permissions for the role
     */
    public Set<Permission> getPermissionsByRoleId(String roleId) {
        if (!roleRepository.existsById(roleId)) {
            throw new ResourceNotFoundException("Role not found with id: " + roleId);
        }

        return permissionRepository.findByRoleId(roleId);
    }

    /**
     * Get permissions by user ID
     *
     * @param userId The user ID
     * @return Set of permissions for the user
     */
    public Set<Permission> getPermissionsByUserId(String userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        return permissionRepository.findByUserId(userId);
    }

    /**
     * Get permissions by username
     *
     * @param username The username
     * @return List of permissions for the user
     */
    public List<Permission> getPermissionsByUsername(String username) {
        if (!userRepository.existsByUsername(username)) {
            throw new ResourceNotFoundException("User not found with username: " + username);
        }

        return permissionRepository.findByUsername(username);
    }

    /**
     * Assign a permission to a role
     *
     * @param permissionId The permission ID
     * @param roleId The role ID
     */
    public void assignPermissionToRole(String permissionId, String roleId) {
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found with id: " + permissionId));

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + roleId));

        // Check if permission is already assigned
        if (role.getPermissions().contains(permission)) {
            throw new ResourceAlreadyExistsException("Permission is already assigned to this role");
        }

        role.addPermission(permission);

        log.debug("Assigning permission {} to role {}", permission.getName(), role.getName());
        roleRepository.save(role);
    }

    /**
     * Remove a permission from a role
     *
     * @param permissionId The permission ID
     * @param roleId The role ID
     */
    public void removePermissionFromRole(String permissionId, String roleId) {
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found with id: " + permissionId));

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + roleId));

        // Check if permission is assigned
        if (!role.getPermissions().contains(permission)) {
            throw new ResourceNotFoundException("Permission is not assigned to this role");
        }

        role.removePermission(permission);

        log.debug("Removing permission {} from role {}", permission.getName(), role.getName());
        roleRepository.save(role);
    }

    /**
     * Get roles that have a specific permission
     *
     * @param permissionName The permission name
     * @return List of roles with the permission
     */
    public List<Role> getRolesByPermission(String permissionName) {
        if (!permissionRepository.existsByName(permissionName)) {
            throw new ResourceNotFoundException("Permission not found with name: " + permissionName);
        }

        return roleRepository.findByPermissionName(permissionName);
    }

    /**
     * Check if a user has a specific permission
     *
     * @param userId The user ID
     * @param permissionName The permission name
     * @return True if the user has the permission
     */
    public boolean hasPermission(String userId, String permissionName) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        if (!permissionRepository.existsByName(permissionName)) {
            throw new ResourceNotFoundException("Permission not found with name: " + permissionName);
        }

        Set<Permission> userPermissions = permissionRepository.findByUserId(userId);
        return userPermissions.stream()
                .anyMatch(permission -> permission.getName().equals(permissionName));
    }

    /**
     * Create CRUD permissions for a resource
     *
     * @param resourceName The resource name
     * @return List of created permissions
     */
    public List<Permission> createCrudPermissions(String resourceName) {
        String readPermission = "READ_" + resourceName.toUpperCase();
        String writePermission = "WRITE_" + resourceName.toUpperCase();
        String updatePermission = "UPDATE_" + resourceName.toUpperCase();
        String deletePermission = "DELETE_" + resourceName.toUpperCase();

        Permission read = Permission.builder()
                .name(readPermission)
                .description("Permission to read " + resourceName)
                .build();

        Permission write = Permission.builder()
                .name(writePermission)
                .description("Permission to create " + resourceName)
                .build();

        Permission update = Permission.builder()
                .name(updatePermission)
                .description("Permission to update " + resourceName)
                .build();

        Permission delete = Permission.builder()
                .name(deletePermission)
                .description("Permission to delete " + resourceName)
                .build();

        List<Permission> permissions = List.of(read, write, update, delete);

        for (Permission permission : permissions) {
            if (permissionRepository.existsByName(permission.getName())) {
                log.debug("Permission {} already exists, skipping creation", permission.getName());
                continue;
            }

            permissionRepository.save(permission);
            log.debug("Created permission: {}", permission.getName());
        }

        return permissions;
    }
}