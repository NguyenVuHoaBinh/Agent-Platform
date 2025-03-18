package viettel.dac.identityservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import viettel.dac.identityservice.exception.ResourceAlreadyExistsException;
import viettel.dac.identityservice.exception.ResourceNotFoundException;
import viettel.dac.identityservice.exception.UnauthorizedException;
import viettel.dac.identityservice.model.Organization;
import viettel.dac.identityservice.model.Project;
import viettel.dac.identityservice.model.User;
import viettel.dac.identityservice.repository.OrganizationRepository;
import viettel.dac.identityservice.repository.ProjectRepository;
import viettel.dac.identityservice.repository.UserRepository;
import viettel.dac.identityservice.security.SecurityUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;

    /**
     * Create a new project
     *
     * @param project The project to create
     * @param organizationId The organization ID
     * @return The created project
     */
    public Project createProject(Project project, String organizationId) {
        // Find organization
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + organizationId));

        // Check if current user is a member of the organization
        String currentUserId = securityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("User not authenticated"));

        if (!organizationRepository.isMember(organizationId, currentUserId)) {
            throw new UnauthorizedException("User is not a member of this organization");
        }

        // Check if project name exists in organization
        if (projectRepository.existsByNameAndOrganizationId(project.getName(), organizationId)) {
            throw new ResourceAlreadyExistsException("Project name already exists in this organization: " + project.getName());
        }

        // Generate ID if not provided
        if (project.getId() == null) {
            project.setId(UUID.randomUUID().toString());
        }

        // Set organization relationship
        project.setOrganization(organization);

        // Add current user as a member
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + currentUserId));

        project.getUsers().add(currentUser);
        currentUser.getProjects().add(project);

        log.debug("Creating new project: {}", project.getName());
        return projectRepository.save(project);
    }

    /**
     * Get a project by ID
     *
     * @param id The project ID
     * @return The project if found
     */
    public Optional<Project> getProjectById(String id) {
        return projectRepository.findById(id);
    }

    /**
     * Get a project with users by ID
     *
     * @param id The project ID
     * @return The project with users if found
     */
    public Optional<Project> getProjectWithUsers(String id) {
        return projectRepository.findByIdWithUsers(id);
    }

    /**
     * Get all projects
     *
     * @param pageable Pagination information
     * @return A page of projects
     */
    public Page<Project> getAllProjects(Pageable pageable) {
        return projectRepository.findAll(pageable);
    }

    /**
     * Search for projects by name or description
     *
     * @param searchTerm The search term
     * @param pageable Pagination information
     * @return A page of matching projects
     */
    public Page<Project> searchProjects(String searchTerm, Pageable pageable) {
        return projectRepository.findBySearchTerm(searchTerm, pageable);
    }

    /**
     * Get projects by organization
     *
     * @param organizationId The organization ID
     * @return List of projects in the organization
     */
    public List<Project> getProjectsByOrganization(String organizationId) {
        if (!organizationRepository.existsById(organizationId)) {
            throw new ResourceNotFoundException("Organization not found with id: " + organizationId);
        }

        return projectRepository.findByOrganizationId(organizationId);
    }

    /**
     * Update a project
     *
     * @param id The project ID
     * @param updatedProject The updated project data
     * @return The updated project
     */
    public Project updateProject(String id, Project updatedProject) {
        Project existingProject = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));

        // Check if current user is a member of the project
        String currentUserId = securityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("User not authenticated"));

        if (!projectRepository.isMember(id, currentUserId)) {
            throw new UnauthorizedException("User is not a member of this project");
        }

        String organizationId = existingProject.getOrganization().getId();

        // Check if name is changed and already exists
        if (!existingProject.getName().equals(updatedProject.getName()) &&
                projectRepository.existsByNameAndOrganizationId(updatedProject.getName(), organizationId)) {
            throw new ResourceAlreadyExistsException("Project name already exists in this organization: " + updatedProject.getName());
        }

        // Update fields
        existingProject.setName(updatedProject.getName());
        existingProject.setDescription(updatedProject.getDescription());

        log.debug("Updating project: {}", existingProject.getName());
        return projectRepository.save(existingProject);
    }

    /**
     * Delete a project
     *
     * @param id The project ID
     */
    public void deleteProject(String id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));

        // Check if current user is a member of the organization
        String currentUserId = securityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("User not authenticated"));

        String organizationId = project.getOrganization().getId();
        if (!organizationRepository.isMember(organizationId, currentUserId)) {
            throw new UnauthorizedException("User is not a member of this organization");
        }

        log.debug("Deleting project: {}", project.getName());
        projectRepository.delete(project);
    }

    /**
     * Get projects for a user
     *
     * @param userId The user ID
     * @return List of projects the user belongs to
     */
    public List<Project> getUserProjects(String userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        return projectRepository.findByUserId(userId);
    }

    /**
     * Get projects for a user in an organization
     *
     * @param userId The user ID
     * @param organizationId The organization ID
     * @return List of projects the user belongs to in the organization
     */
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
     * Add a user to a project
     *
     * @param projectId The project ID
     * @param userId The user ID
     * @return The updated project
     */
    public Project addUserToProject(String projectId, String userId) {
        Project project = projectRepository.findByIdWithUsers(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Check if user is already a member
        if (projectRepository.isMember(projectId, userId)) {
            throw new ResourceAlreadyExistsException("User is already a member of this project");
        }

        // Check if user is a member of the organization
        String organizationId = project.getOrganization().getId();
        if (!organizationRepository.isMember(organizationId, userId)) {
            throw new UnauthorizedException("User must be a member of the organization first");
        }

        project.getUsers().add(user);
        user.getProjects().add(project);

        log.debug("Adding user {} to project {}", user.getUsername(), project.getName());
        return projectRepository.save(project);
    }

    /**
     * Remove a user from a project
     *
     * @param projectId The project ID
     * @param userId The user ID
     * @return The updated project
     */
    public Project removeUserFromProject(String projectId, String userId) {
        Project project = projectRepository.findByIdWithUsers(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Check if user is a member
        if (!projectRepository.isMember(projectId, userId)) {
            throw new ResourceNotFoundException("User is not a member of this project");
        }

        // Ensure at least one user remains in the project
        if (project.getUsers().size() <= 1) {
            throw new IllegalStateException("Cannot remove the last user from a project");
        }

        project.getUsers().remove(user);
        user.getProjects().remove(project);

        log.debug("Removing user {} from project {}", user.getUsername(), project.getName());
        return projectRepository.save(project);
    }

    /**
     * Check if a user is a member of a project
     *
     * @param projectId The project ID
     * @param userId The user ID
     * @return True if the user is a member
     */
    public boolean isUserMemberOfProject(String projectId, String userId) {
        return projectRepository.isMember(projectId, userId);
    }

    /**
     * Get members of a project
     *
     * @param projectId The project ID
     * @return List of users in the project
     */
    public List<User> getProjectMembers(String projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Project not found with id: " + projectId);
        }

        return userRepository.findByProjectId(projectId);
    }
}