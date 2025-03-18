package viettel.dac.identityservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import viettel.dac.identityservice.exception.ResourceAlreadyExistsException;
import viettel.dac.identityservice.exception.ResourceNotFoundException;
import viettel.dac.identityservice.model.Permission;
import viettel.dac.identityservice.model.Role;
import viettel.dac.identityservice.model.User;
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
public class RoleService {
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;

    /**
     * Create a new role
     *
     * @param role The role to create
     * @return The created role
     */
    public Role createRole(Role role) {
        // Check if role name already exists
        if (roleRepository.existsByName(role.getName())) {
            throw new ResourceAlreadyExistsException("Role name already exists: " + role.getName());
        }

        // Generate ID if not provided
        if (role.getId() == null) {
            role.setId(UUID.randomUUID().toString());
        }

        log.debug("Creating new role: {}", role.getName());
        return roleRepository.save(role);
    }

    /**
     * Get a role by ID
     *
     * @param id The role ID
     * @return The role if found
     */
    public Optional<Role> getRoleById(String id) {
        return roleRepository.findById(id);
    }

    /**
     * Get a role by ID with permissions
     *
     * @param id The role ID
     * @return The role with permissions if found
     */
    public Optional<Role> getRoleByIdWithPermissions(String id) {
        return roleRepository.findByIdWithPermissions(id);
    }

    /**
     * Get a role by name
     *
     * @param name The role name
     * @return The role if found
     */
    public Optional<Role> getRoleByName(String name) {
        return roleRepository.findByName(name);
    }

    /**
     * Get all roles
     *
     * @return List of all roles
     */
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    /**
     * Get all roles with their permissions
     *
     * @return List of all roles with permissions
     */
    public List<Role> getAllRolesWithPermissions() {
        return roleRepository.findAllWithPermissions();
    }

    /**
     * Update a role
     *
     * @param id The role ID
     * @param updatedRole The updated role data
     * @return The updated role
     */
    public Role updateRole(String id, Role updatedRole) {
        Role existingRole = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + id));

        // Check if name is changed and already exists
        if (!existingRole.getName().equals(updatedRole.getName()) &&
                roleRepository.existsByName(updatedRole.getName())) {
            throw new ResourceAlreadyExistsException("Role name already exists: " + updatedRole.getName());
        }

        // Update fields
        existingRole.setName(updatedRole.getName());
        existingRole.setDescription(updatedRole.getDescription());

        log.debug("Updating role: {}", existingRole.getName());
        return roleRepository.save(existingRole);
    }

    /**
     * Delete a role
     *
     * @param id The role ID
     */
    public void deleteRole(String id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + id));

        // Check if role is assigned to any users
        if (!role.getUsers().isEmpty()) {
            throw new IllegalStateException("Cannot delete role that is assigned to users");
        }

        log.debug("Deleting role: {}", role.getName());
        roleRepository.delete(role);
    }

    /**
     * Add a permission to a role
     *
     * @param roleId The role ID
     * @param permissionId The permission ID
     * @return The updated role
     */
    public Role addPermissionToRole(String roleId, String permissionId) {
        Role role = roleRepository.findByIdWithPermissions(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + roleId));

        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found with id: " + permissionId));

        // Check if permission is already assigned
        if (role.getPermissions().contains(permission)) {
            throw new ResourceAlreadyExistsException("Permission is already assigned to this role");
        }

        role.addPermission(permission);

        log.debug("Adding permission {} to role {}", permission.getName(), role.getName());
        return roleRepository.save(role);
    }

    /**
     * Add a permission to a role by name
     *
     * @param roleId The role ID
     * @param permissionName The permission name
     * @return The updated role
     */
    public Role addPermissionToRoleByName(String roleId, String permissionName) {
        Role role = roleRepository.findByIdWithPermissions(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + roleId));

        Permission permission = permissionRepository.findByName(permissionName)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found with name: " + permissionName));

        // Check if permission is already assigned
        if (role.getPermissions().contains(permission)) {
            throw new ResourceAlreadyExistsException("Permission is already assigned to this role");
        }

        role.addPermission(permission);

        log.debug("Adding permission {} to role {}", permissionName, role.getName());
        return roleRepository.save(role);
    }

    /**
     * Remove a permission from a role
     *
     * @param roleId The role ID
     * @param permissionId The permission ID
     * @return The updated role
     */
    public Role removePermissionFromRole(String roleId, String permissionId) {
        Role role = roleRepository.findByIdWithPermissions(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + roleId));

        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found with id: " + permissionId));

        // Check if permission is assigned
        if (!role.getPermissions().contains(permission)) {
            throw new ResourceNotFoundException("Permission is not assigned to this role");
        }

        role.removePermission(permission);

        log.debug("Removing permission {} from role {}", permission.getName(), role.getName());
        return roleRepository.save(role);
    }

    /**
     * Remove a permission from a role by name
     *
     * @param roleId The role ID
     * @param permissionName The permission name
     * @return The updated role
     */
    public Role removePermissionFromRoleByName(String roleId, String permissionName) {
        Role role = roleRepository.findByIdWithPermissions(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + roleId));

        Permission permission = permissionRepository.findByName(permissionName)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found with name: " + permissionName));

        // Check if permission is assigned
        if (!role.getPermissions().contains(permission)) {
            throw new ResourceNotFoundException("Permission is not assigned to this role");
        }

        role.removePermission(permission);

        log.debug("Removing permission {} from role {}", permissionName, role.getName());
        return roleRepository.save(role);
    }

    /**
     * Get permissions for a role
     *
     * @param roleId The role ID
     * @return Set of permissions assigned to the role
     */
    public Set<Permission> getRolePermissions(String roleId) {
        Role role = roleRepository.findByIdWithPermissions(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + roleId));

        return role.getPermissions();
    }

    /**
     * Get roles for a user
     *
     * @param userId The user ID
     * @return Set of roles assigned to the user
     */
    public Set<Role> getUserRoles(String userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        return roleRepository.findByUserId(userId);
    }

    /**
     * Assign a role to a user
     *
     * @param roleId The role ID
     * @param userId The user ID
     */
    public void assignRoleToUser(String roleId, String userId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + roleId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Check if role is already assigned
        if (user.getRoles().contains(role)) {
            throw new ResourceAlreadyExistsException("Role is already assigned to this user");
        }

        user.addRole(role);

        log.debug("Assigning role {} to user {}", role.getName(), user.getUsername());
        userRepository.save(user);
    }

    /**
     * Remove a role from a user
     *
     * @param roleId The role ID
     * @param userId The user ID
     */
    public void removeRoleFromUser(String roleId, String userId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + roleId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Check if role is assigned
        if (!user.getRoles().contains(role)) {
            throw new ResourceNotFoundException("Role is not assigned to this user");
        }

        // Ensure user has at least one role
        if (user.getRoles().size() <= 1) {
            throw new IllegalStateException("Cannot remove the last role from a user");
        }

        user.removeRole(role);

        log.debug("Removing role {} from user {}", role.getName(), user.getUsername());
        userRepository.save(user);
    }

    /**
     * Get users with a specific role
     *
     * @param roleName The role name
     * @return List of users with the role
     */
    public List<User> getUsersByRole(String roleName) {
        if (!roleRepository.existsByName(roleName)) {
            throw new ResourceNotFoundException("Role not found with name: " + roleName);
        }

        return userRepository.findByRoleName(roleName);
    }
}