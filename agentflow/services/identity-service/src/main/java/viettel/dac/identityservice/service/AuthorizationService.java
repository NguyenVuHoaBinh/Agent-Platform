package viettel.dac.identityservice.service;

import viettel.dac.identityservice.entity.Permission;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Service for handling authorization and permissions
 */
public interface AuthorizationService {

    /**
     * Checks if a user has a specific permission for a resource
     *
     * @param userId User ID to check
     * @param permissionName Permission name to check
     * @param resourceId Resource ID (optional)
     * @param resourceType Resource type (optional)
     * @return true if the user has the permission, false otherwise
     */
    boolean hasPermission(String userId, String permissionName, String resourceId, String resourceType);

    /**
     * Gets all permissions for a user in a specific context
     *
     * @param userId User ID
     * @param organizationId Organization ID (optional)
     * @param projectId Project ID (optional)
     * @return List of permissions
     */
    List<Permission> getUserPermissions(String userId, String organizationId, String projectId);

    /**
     * Gets a map of resources and their permissions for a user
     *
     * @param userId User ID
     * @return Map of resource keys to permission sets
     */
    Map<String, Set<String>> getUserResourcePermissions(String userId);
}