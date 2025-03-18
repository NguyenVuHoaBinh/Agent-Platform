package viettel.dac.identityservice.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import viettel.dac.identityservice.exception.EmailAlreadyExistsException;
import viettel.dac.identityservice.exception.ResourceNotFoundException;
import viettel.dac.identityservice.exception.UsernameAlreadyExistsException;
import viettel.dac.identityservice.model.Organization;
import viettel.dac.identityservice.model.Project;
import viettel.dac.identityservice.model.Role;
import viettel.dac.identityservice.model.User;
import viettel.dac.identityservice.repository.OrganizationRepository;
import viettel.dac.identityservice.repository.ProjectRepository;
import viettel.dac.identityservice.repository.RoleRepository;
import viettel.dac.identityservice.repository.UserRepository;
import viettel.dac.identityservice.security.SecurityUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OrganizationRepository organizationRepository;
    private final ProjectRepository projectRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityUtils securityUtils;

    /**
     * Create a new user
     *
     * @param user The user to create
     * @return The created user
     */
    public User createUser(User user) {
        // Check if username or email already exists
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new UsernameAlreadyExistsException("Username already in use: " + user.getUsername());
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new EmailAlreadyExistsException("Email already in use: " + user.getEmail());
        }

        // Encode password
        user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));

        // Assign default role if none specified
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            roleRepository.findByName("ROLE_USER")
                    .ifPresent(user::addRole);
        }

        log.debug("Creating new user: {}", user.getUsername());
        return userRepository.save(user);
    }

    /**
     * Get a user by ID
     *
     * @param id The user ID
     * @return The user if found
     */
    public Optional<User> getUserById(String id) {
        return userRepository.findById(id);
    }

    /**
     * Get a user by ID with roles
     *
     * @param id The user ID
     * @return The user with roles if found
     */
    public Optional<User> getUserByIdWithRoles(String id) {
        return userRepository.findByIdWithRoles(id);
    }

    /**
     * Get a user by username
     *
     * @param username The username
     * @return The user if found
     */
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Get a user by email
     *
     * @param email The email
     * @return The user if found
     */
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Get all users
     *
     * @param pageable Pagination information
     * @return A page of users
     */
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    /**
     * Search for users by name, username, or email
     *
     * @param searchTerm The search term
     * @param pageable Pagination information
     * @return A page of matching users
     */
    public Page<User> searchUsers(String searchTerm, Pageable pageable) {
        return userRepository.findBySearchTerm(searchTerm, pageable);
    }

    /**
     * Update a user's information
     *
     * @param id The user ID
     * @param updatedUser The updated user data
     * @return The updated user
     */
    public User updateUser(String id, User updatedUser) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // Check if username is changed and already exists
        if (!existingUser.getUsername().equals(updatedUser.getUsername()) &&
                userRepository.existsByUsername(updatedUser.getUsername())) {
            throw new UsernameAlreadyExistsException("Username already in use: " + updatedUser.getUsername());
        }

        // Check if email is changed and already exists
        if (!existingUser.getEmail().equals(updatedUser.getEmail()) &&
                userRepository.existsByEmail(updatedUser.getEmail())) {
            throw new EmailAlreadyExistsException("Email already in use: " + updatedUser.getEmail());
        }

        // Update fields
        existingUser.setUsername(updatedUser.getUsername());
        existingUser.setEmail(updatedUser.getEmail());
        existingUser.setFirstName(updatedUser.getFirstName());
        existingUser.setLastName(updatedUser.getLastName());

        // Only update password if it's provided
        if (updatedUser.getPasswordHash() != null && !updatedUser.getPasswordHash().isEmpty()) {
            existingUser.setPasswordHash(passwordEncoder.encode(updatedUser.getPasswordHash()));
        }

        existingUser.setEnabled(updatedUser.isEnabled());
        existingUser.setAccountNonExpired(updatedUser.isAccountNonExpired());
        existingUser.setAccountNonLocked(updatedUser.isAccountNonLocked());
        existingUser.setCredentialsNonExpired(updatedUser.isCredentialsNonExpired());

        log.debug("Updating user: {}", existingUser.getUsername());
        return userRepository.save(existingUser);
    }

    /**
     * Delete a user
     *
     * @param id The user ID
     */
    public void deleteUser(String id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        log.debug("Deleting user with ID: {}", id);
        userRepository.deleteById(id);
    }

    /**
     * Add a role to a user
     *
     * @param userId The user ID
     * @param roleName The role name
     * @return The updated user
     */
    public User addRoleToUser(String userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with name: " + roleName));

        user.addRole(role);
        log.debug("Adding role {} to user {}", roleName, user.getUsername());
        return userRepository.save(user);
    }

    /**
     * Remove a role from a user
     *
     * @param userId The user ID
     * @param roleName The role name
     * @return The updated user
     */
    public User removeRoleFromUser(String userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with name: " + roleName));

        user.removeRole(role);
        log.debug("Removing role {} from user {}", roleName, user.getUsername());
        return userRepository.save(user);
    }

    /**
     * Get all roles for a user
     *
     * @param userId The user ID
     * @return The user's roles
     */
    public Set<Role> getUserRoles(String userId) {
        return roleRepository.findByUserId(userId);
    }

    /**
     * Add a user to an organization
     *
     * @param userId The user ID
     * @param organizationId The organization ID
     * @return The updated user
     */
    public User addUserToOrganization(String userId, String organizationId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + organizationId));

        user.addOrganization(organization);
        log.debug("Adding user {} to organization {}", user.getUsername(), organization.getName());
        return userRepository.save(user);
    }

    /**
     * Remove a user from an organization
     *
     * @param userId The user ID
     * @param organizationId The organization ID
     * @return The updated user
     */
    public User removeUserFromOrganization(String userId, String organizationId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + organizationId));

        user.removeOrganization(organization);

        // Also remove user from all projects in this organization
        List<Project> organizationProjects = projectRepository.findByOrganizationId(organizationId);
        for (Project project : organizationProjects) {
            if (project.getUsers().contains(user)) {
                user.removeProject(project);
            }
        }

        log.debug("Removing user {} from organization {}", user.getUsername(), organization.getName());
        return userRepository.save(user);
    }

    /**
     * Get all organizations for a user
     *
     * @param userId The user ID
     * @return The user's organizations
     */
    public List<Organization> getUserOrganizations(String userId) {
        return organizationRepository.findByUserId(userId);
    }

    /**
     * Add a user to a project
     *
     * @param userId The user ID
     * @param projectId The project ID
     * @return The updated user
     */
    public User addUserToProject(String userId, String projectId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        // Check if user is part of the organization
        Organization organization = project.getOrganization();
        if (!organizationRepository.isMember(organization.getId(), userId)) {
            throw new IllegalStateException("User must be a member of the organization first");
        }

        user.addProject(project);
        log.debug("Adding user {} to project {}", user.getUsername(), project.getName());
        return userRepository.save(user);
    }

    /**
     * Remove a user from a project
     *
     * @param userId The user ID
     * @param projectId The project ID
     * @return The updated user
     */
    public User removeUserFromProject(String userId, String projectId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        user.removeProject(project);
        log.debug("Removing user {} from project {}", user.getUsername(), project.getName());
        return userRepository.save(user);
    }

    /**
     * Get all projects for a user
     *
     * @param userId The user ID
     * @return The user's projects
     */
    public List<Project> getUserProjects(String userId) {
        return projectRepository.findByUserId(userId);
    }

    /**
     * Get all projects for a user in an organization
     *
     * @param userId The user ID
     * @param organizationId The organization ID
     * @return The user's projects in the organization
     */
    public List<Project> getUserProjectsInOrganization(String userId, String organizationId) {
        return projectRepository.findByUserIdAndOrganizationId(userId, organizationId);
    }

    /**
     * Change a user's password
     *
     * @param userId The user ID
     * @param currentPassword The current password
     * @param newPassword The new password
     * @return The updated user
     */
    public User changePassword(String userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        // Update password
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        log.debug("Changing password for user: {}", user.getUsername());
        return userRepository.save(user);
    }

    /**
     * Check if a user has a specific role
     *
     * @param userId The user ID
     * @param roleName The role name
     * @return True if the user has the role
     */
    public boolean hasRole(String userId, String roleName) {
        return userRepository.hasRole(userId, roleName);
    }
}