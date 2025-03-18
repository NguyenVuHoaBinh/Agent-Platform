package viettel.dac.identityservice.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import viettel.dac.identityservice.service.OrganizationService;
import viettel.dac.identityservice.service.ProjectService;

/**
 * Security expression methods for use in @PreAuthorize annotations
 */
@Component
@RequiredArgsConstructor
public class SecurityExpressionMethods {

    private final OrganizationService organizationService;
    private final ProjectService projectService;
    private final SecurityUtils securityUtils;

    /**
     * Check if the current authenticated user is the requested user
     *
     * @param userId The user ID to check against
     * @return true if the current user is the requested user
     */
    public boolean isCurrentUser(String userId) {
        return securityUtils.getCurrentUserId()
                .map(id -> id.equals(userId))
                .orElse(false);
    }

    /**
     * Check if the current authenticated user is a member of the organization
     *
     * @param organizationId The organization ID
     * @return true if the current user is a member of the organization
     */
    public boolean isOrganizationMember(String organizationId) {
        return securityUtils.getCurrentUserId()
                .map(userId -> organizationService.isUserMemberOfOrganization(organizationId, userId))
                .orElse(false);
    }

    /**
     * Check if the current authenticated user is a member of the project
     *
     * @param projectId The project ID
     * @return true if the current user is a member of the project
     */
    public boolean isProjectMember(String projectId) {
        return securityUtils.getCurrentUserId()
                .map(userId -> projectService.isUserMemberOfProject(projectId, userId))
                .orElse(false);
    }

    /**
     * Get the current authenticated user ID
     *
     * @return The current user ID or null if not authenticated
     */
    public String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() &&
                authentication.getPrincipal() instanceof UserDetailsImpl) {
            return ((UserDetailsImpl) authentication.getPrincipal()).getId();
        }
        return null;
    }
}