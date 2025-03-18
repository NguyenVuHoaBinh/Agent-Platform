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
import viettel.dac.identityservice.model.User;
import viettel.dac.identityservice.repository.OrganizationRepository;
import viettel.dac.identityservice.repository.UserRepository;
import viettel.dac.identityservice.security.SecurityUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class OrganizationService {
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;

    /**
     * Create a new organization
     *
     * @param organization The organization to create
     * @return The created organization
     */
    public Organization createOrganization(Organization organization) {
        // Check if organization name is available
        if (organizationRepository.existsByName(organization.getName())) {
            throw new ResourceAlreadyExistsException("Organization name already exists: " + organization.getName());
        }

        // Generate ID if not provided
        if (organization.getId() == null) {
            organization.setId(UUID.randomUUID().toString());
        }

        // Add current user as a member
        String currentUserId = securityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("User not authenticated"));

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + currentUserId));

        organization.getUsers().add(currentUser);
        currentUser.getOrganizations().add(organization);

        log.debug("Creating new organization: {}", organization.getName());
        return organizationRepository.save(organization);
    }

    /**
     * Get an organization by ID
     *
     * @param id The organization ID
     * @return The organization if found
     */
    public Optional<Organization> getOrganizationById(String id) {
        return organizationRepository.findById(id);
    }

    /**
     * Get an organization with projects by ID
     *
     * @param id The organization ID
     * @return The organization with projects if found
     */
    public Optional<Organization> getOrganizationWithProjects(String id) {
        return organizationRepository.findByIdWithProjects(id);
    }

    /**
     * Get all organizations
     *
     * @param pageable Pagination information
     * @return A page of organizations
     */
    public Page<Organization> getAllOrganizations(Pageable pageable) {
        return organizationRepository.findAll(pageable);
    }

    /**
     * Search for organizations by name
     *
     * @param searchTerm The search term
     * @param pageable Pagination information
     * @return A page of matching organizations
     */
    public Page<Organization> searchOrganizations(String searchTerm, Pageable pageable) {
        return organizationRepository.findBySearchTerm(searchTerm, pageable);
    }

    /**
     * Update an organization
     *
     * @param id The organization ID
     * @param updatedOrganization The updated organization data
     * @return The updated organization
     */
    public Organization updateOrganization(String id, Organization updatedOrganization) {
        Organization existingOrganization = organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + id));

        // Check if name is changed and already exists
        if (!existingOrganization.getName().equals(updatedOrganization.getName()) &&
                organizationRepository.existsByName(updatedOrganization.getName())) {
            throw new ResourceAlreadyExistsException("Organization name already exists: " + updatedOrganization.getName());
        }

        // Update fields
        existingOrganization.setName(updatedOrganization.getName());
        existingOrganization.setDescription(updatedOrganization.getDescription());

        log.debug("Updating organization: {}", existingOrganization.getName());
        return organizationRepository.save(existingOrganization);
    }

    /**
     * Delete an organization
     *
     * @param id The organization ID
     */
    public void deleteOrganization(String id) {
        if (!organizationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Organization not found with id: " + id);
        }

        log.debug("Deleting organization with ID: {}", id);
        organizationRepository.deleteById(id);
    }

    /**
     * Get organizations for a user
     *
     * @param userId The user ID
     * @return List of organizations the user belongs to
     */
    public List<Organization> getUserOrganizations(String userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        return organizationRepository.findByUserId(userId);
    }

    /**
     * Add a user to an organization
     *
     * @param organizationId The organization ID
     * @param userId The user ID
     * @return The updated organization
     */
    public Organization addUserToOrganization(String organizationId, String userId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + organizationId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Check if user is already a member
        if (organizationRepository.isMember(organizationId, userId)) {
            throw new ResourceAlreadyExistsException("User is already a member of this organization");
        }

        organization.getUsers().add(user);
        user.getOrganizations().add(organization);

        log.debug("Adding user {} to organization {}", user.getUsername(), organization.getName());
        return organizationRepository.save(organization);
    }

    /**
     * Remove a user from an organization
     *
     * @param organizationId The organization ID
     * @param userId The user ID
     * @return The updated organization
     */
    public Organization removeUserFromOrganization(String organizationId, String userId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + organizationId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Check if user is a member
        if (!organizationRepository.isMember(organizationId, userId)) {
            throw new ResourceNotFoundException("User is not a member of this organization");
        }

        // Ensure at least one user remains in the organization
        if (organization.getUsers().size() <= 1) {
            throw new IllegalStateException("Cannot remove the last user from an organization");
        }

        organization.getUsers().remove(user);
        user.getOrganizations().remove(organization);

        // Remove user from all projects in this organization
        user.getProjects().removeIf(project -> project.getOrganization().getId().equals(organizationId));

        log.debug("Removing user {} from organization {}", user.getUsername(), organization.getName());
        return organizationRepository.save(organization);
    }

    /**
     * Check if a user is a member of an organization
     *
     * @param organizationId The organization ID
     * @param userId The user ID
     * @return True if the user is a member
     */
    public boolean isUserMemberOfOrganization(String organizationId, String userId) {
        return organizationRepository.isMember(organizationId, userId);
    }

    /**
     * Get members of an organization
     *
     * @param organizationId The organization ID
     * @return List of users in the organization
     */
    public List<User> getOrganizationMembers(String organizationId) {
        if (!organizationRepository.existsById(organizationId)) {
            throw new ResourceNotFoundException("Organization not found with id: " + organizationId);
        }

        return userRepository.findByOrganizationId(organizationId);
    }
}