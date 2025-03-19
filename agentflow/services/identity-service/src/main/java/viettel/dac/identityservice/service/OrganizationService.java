package viettel.dac.identityservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
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

/**
 * Service for organization management
 * With improved transaction boundaries
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OrganizationService {
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;
    private final UserOrganizationService userOrganizationService;

    /**
     * Create a new organization with current user as member
     * Critical operation that creates multiple relationships
     *
     * @param organization The organization to create
     * @return The created organization
     */
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
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
     * Read-only method
     *
     * @param id The organization ID
     * @return The organization if found
     */
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public Optional<Organization> getOrganizationById(String id) {
        return organizationRepository.findById(id);
    }

    /**
     * Get an organization with projects by ID
     * Read-only method that fetches additional data
     *
     * @param id The organization ID
     * @return The organization with projects if found
     */
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public Optional<Organization> getOrganizationWithProjects(String id) {
        return organizationRepository.findByIdWithProjects(id);
    }

    /**
     * Get all organizations with pagination
     * Read-only method
     *
     * @param pageable Pagination information
     * @return A page of organizations
     */
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public Page<Organization> getAllOrganizations(Pageable pageable) {
        return organizationRepository.findAll(pageable);
    }

    /**
     * Search for organizations by name
     * Read-only method
     *
     * @param searchTerm The search term
     * @param pageable   Pagination information
     * @return A page of matching organizations
     */
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public Page<Organization> searchOrganizations(String searchTerm, Pageable pageable) {
        return organizationRepository.findBySearchTerm(searchTerm, pageable);
    }

    /**
     * Update an organization
     * Write operation that requires existing transaction or creates a new one
     *
     * @param id                  The organization ID
     * @param updatedOrganization The updated organization data
     * @return The updated organization
     */
    @Transactional(propagation = Propagation.REQUIRED)
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
     * Critical operation that requires its own transaction
     *
     * @param id The organization ID
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void deleteOrganization(String id) {
        if (!organizationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Organization not found with id: " + id);
        }

        log.debug("Deleting organization with ID: {}", id);
        organizationRepository.deleteById(id);
    }

    /**
     * Add a user to an organization
     * Delegates to UserOrganizationService
     *
     * @param organizationId The organization ID
     * @param userId         The user ID
     * @return The updated organization
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public Organization addUserToOrganization(String organizationId, String userId) {
        userOrganizationService.addUserToOrganization(userId, organizationId);
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + organizationId));
    }

    /**
     * Remove a user from an organization
     * Delegates to UserOrganizationService
     *
     * @param organizationId The organization ID
     * @param userId         The user ID
     * @return The updated organization
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public Organization removeUserFromOrganization(String organizationId, String userId) {
        userOrganizationService.removeUserFromOrganization(userId, organizationId);
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + organizationId));
    }

    /**
     * Check if a user is a member of an organization
     * Delegates to UserOrganizationService
     *
     * @param organizationId The organization ID
     * @param userId         The user ID
     * @return True if the user is a member
     */
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public boolean isUserMemberOfOrganization(String organizationId, String userId) {
        return userOrganizationService.isUserMemberOfOrganization(organizationId, userId);
    }

    /**
     * Get members of an organization
     * Delegates to UserOrganizationService
     *
     * @param organizationId The organization ID
     * @return List of users in the organization
     */
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public List<User> getOrganizationMembers(String organizationId) {
        return userOrganizationService.getOrganizationMembers(organizationId);
    }
}