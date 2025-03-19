package viettel.dac.identityservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import viettel.dac.identityservice.exception.ResourceAlreadyExistsException;
import viettel.dac.identityservice.exception.ResourceNotFoundException;
import viettel.dac.identityservice.model.Organization;
import viettel.dac.identityservice.model.Project;
import viettel.dac.identityservice.model.User;
import viettel.dac.identityservice.repository.OrganizationRepository;
import viettel.dac.identityservice.repository.ProjectRepository;
import viettel.dac.identityservice.repository.UserRepository;

import java.util.List;

/**
 * Service for managing the relationships between users, organizations, and projects.
 * With improved transaction management for critical operations.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserOrganizationService {
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final ProjectRepository projectRepository;

    /**
     * Add a user to an organization
     * Uses REQUIRED propagation to join an existing transaction or create a new one
     *
     * @param userId The user ID
     * @param organizationId The organization ID
     * @return The updated user
     */
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public User addUserToOrganization(String userId, String organizationId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + organizationId));

        // Check if user is already a member - use repository method to avoid inconsistencies
        if (organizationRepository.isMember(organizationId, userId)) {
            throw new ResourceAlreadyExistsException("User is already a member of this organization");
        }

        user.addOrganization(organization);
        log.debug("Adding user {} to organization {}", user.getUsername(), organization.getName());
        return userRepository.save(user);
    }

    /**
     * Remove a user from an organization and all its projects
     * Uses READ_COMMITTED isolation to prevent dirty reads and REQUIRED propagation
     * This is a critical operation that modifies multiple related entities
     *
     * @param userId The user ID
     * @param organizationId The organization ID
     * @return The updated user
     */
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public User removeUserFromOrganization(String userId, String organizationId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + organizationId));

        // Check if user is a member
        if (!organizationRepository.isMember(organizationId, userId)) {
            throw new ResourceNotFoundException("User is not a member of this organization");
        }

        // Ensure at least one user remains in the organization
        if (organization.getUsers().size() <= 1) {
            throw new IllegalStateException("Cannot remove the last user from an organization");
        }

        // Remove user from organization
        user.removeOrganization(organization);

        // Get all projects for this organization
        List<Project> organizationProjects = projectRepository.findByOrganizationId(organizationId);

        // Remove user from all projects in this organization
        for (Project project : organizationProjects) {
            if (project.getUsers().contains(user)) {
                user.removeProject(project);
                // We don't need to save the project as the user entity owns the relationship
            }
        }

        log.debug("Removing user {} from organization {} and its projects", user.getUsername(), organization.getName());
        return userRepository.save(user);
    }

    /**
     * Add a user to a project
     * Uses REQUIRED propagation and READ_COMMITTED isolation
     * Validates organization membership as a precondition
     *
     * @param userId The user ID
     * @param projectId The project ID
     * @return The updated user
     */
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public User addUserToProject(String userId, String projectId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        // Check if user is already a member
        if (projectRepository.isMember(projectId, userId)) {
            throw new ResourceAlreadyExistsException("User is already a member of this project");
        }

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
     * Uses REQUIRED propagation and READ_COMMITTED isolation
     *
     * @param userId The user ID
     * @param projectId The project ID
     * @return The updated user
     */
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public User removeUserFromProject(String userId, String projectId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        // Check if user is a member
        if (!projectRepository.isMember(projectId, userId)) {
            throw new ResourceNotFoundException("User is not a member of this project");
        }

        // Ensure at least one user remains in the project
        if (project.getUsers().size() <= 1) {
            throw new IllegalStateException("Cannot remove the last user from a project");
        }

        user.removeProject(project);
        log.debug("Removing user {} from project {}", user.getUsername(), project.getName());
        return userRepository.save(user);
    }

    /**
     * Check if a user is a member of an organization
     * Read-only method so uses SUPPORTS propagation to join existing transaction if present
     *
     * @param organizationId The organization ID
     * @param userId The user ID
     * @return True if the user is a member
     */
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public boolean isUserMemberOfOrganization(String organizationId, String userId) {
        return organizationRepository.isMember(organizationId, userId);
    }

    /**
     * Check if a user is a member of a project
     * Read-only method so uses SUPPORTS propagation to join existing transaction if present
     *
     * @param projectId The project ID
     * @param userId The user ID
     * @return True if the user is a member
     */
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public boolean isUserMemberOfProject(String projectId, String userId) {
        return projectRepository.isMember(projectId, userId);
    }

    /**
     * Get organizations for a user
     * Read-only operation so uses readOnly=true and SUPPORTS propagation
     *
     * @param userId The user ID
     * @return List of organizations the user belongs to
     */
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public List<Organization> getUserOrganizations(String userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        return organizationRepository.findByUserId(userId);
    }

    /**
     * Get projects for a user
     * Read-only operation so uses readOnly=true and SUPPORTS propagation
     *
     * @param userId The user ID
     * @return List of projects the user belongs to
     */
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public List<Project> getUserProjects(String userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        return projectRepository.findByUserId(userId);
    }

    /**
     * Get projects for a user in an organization
     * Read-only operation so uses readOnly=true and SUPPORTS propagation
     *
     * @param userId The user ID
     * @param organizationId The organization ID
     * @return List of projects the user belongs to in the organization
     */
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public List<Project> getUserProjectsInOrganization(String userId, String organizationId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        if (!organizationRepository.existsById(organizationId)) {
            throw new ResourceNotFoundException("Organization not found with id: " + organizationId);
        }

        return projectRepository.findByUserIdAndOrganizationId(userId, organizationId);
    }

    /**
     * Get members of an organization
     * Read-only operation so uses readOnly=true and SUPPORTS propagation
     *
     * @param organizationId The organization ID
     * @return List of users in the organization
     */
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public List<User> getOrganizationMembers(String organizationId) {
        if (!organizationRepository.existsById(organizationId)) {
            throw new ResourceNotFoundException("Organization not found with id: " + organizationId);
        }

        return userRepository.findByOrganizationId(organizationId);
    }

    /**
     * Get members of a project
     * Read-only operation so uses readOnly=true and SUPPORTS propagation
     *
     * @param projectId The project ID
     * @return List of users in the project
     */
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public List<User> getProjectMembers(String projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Project not found with id: " + projectId);
        }

        return userRepository.findByProjectId(projectId);
    }
}