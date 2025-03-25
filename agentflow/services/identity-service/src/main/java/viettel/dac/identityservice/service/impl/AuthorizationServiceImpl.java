package viettel.dac.identityservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import viettel.dac.identityservice.entity.Permission;
import viettel.dac.identityservice.entity.Project;
import viettel.dac.identityservice.repository.PermissionRepository;
import viettel.dac.identityservice.repository.ProjectRepository;
import viettel.dac.identityservice.repository.UserRoleRepository;
import viettel.dac.identityservice.service.AuthorizationService;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthorizationServiceImpl implements AuthorizationService {

    private final UserRoleRepository userRoleRepository;
    private final PermissionRepository permissionRepository;
    private final ProjectRepository projectRepository;

    @Override
    public boolean hasPermission(String userId, String permissionName, String resourceId, String resourceType) {
        // First check for global permissions (super admin)
        Set<String> globalPermissions = userRoleRepository.findGlobalRolesByUserId(userId).stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getName)
                .collect(Collectors.toSet());

        if (globalPermissions.contains("*") || globalPermissions.contains(permissionName)) {
            return true;
        }

        // If no resource context is specified, check organizational permissions
        if (resourceId == null) {
            return false;
        }

        // Check organization-specific permissions
        if ("organization".equals(resourceType)) {
            return userRoleRepository.findByUserIdAndOrganizationId(userId, resourceId).stream()
                    .flatMap(role -> role.getRole().getPermissions().stream())
                    .map(Permission::getName)
                    .anyMatch(perm -> perm.equals(permissionName) || perm.equals("*"));
        }

        // Check project-specific permissions
        if ("project".equals(resourceType)) {
            // Check direct project permissions
            boolean hasDirectProjectPermission = userRoleRepository.findByUserIdAndProjectId(userId, resourceId).stream()
                    .flatMap(role -> role.getRole().getPermissions().stream())
                    .map(Permission::getName)
                    .anyMatch(perm -> perm.equals(permissionName) || perm.equals("*"));

            if (hasDirectProjectPermission) {
                return true;
            }

            // Check if user has org-level permission for the project's organization
            Optional<Project> project = projectRepository.findById(resourceId);
            if (project.isPresent()) {
                String organizationId = project.get().getOrganization().getId();
                return userRoleRepository.findByUserIdAndOrganizationId(userId, organizationId).stream()
                        .flatMap(role -> role.getRole().getPermissions().stream())
                        .map(Permission::getName)
                        .anyMatch(perm -> perm.equals(permissionName) || perm.equals("*"));
            }
        }

        return false;
    }

    @Override
    public List<Permission> getUserPermissions(String userId, String organizationId, String projectId) {
        List<Permission> permissions = new ArrayList<>();

        // Get global permissions
        userRoleRepository.findGlobalRolesByUserId(userId).stream()
                .flatMap(role -> role.getPermissions().stream())
                .forEach(permissions::add);

        // Get organization-specific permissions if organization ID is provided
        if (organizationId != null) {
            userRoleRepository.findByUserIdAndOrganizationId(userId, organizationId).stream()
                    .flatMap(userRole -> userRole.getRole().getPermissions().stream())
                    .forEach(permissions::add);
        }

        // Get project-specific permissions if project ID is provided
        if (projectId != null) {
            userRoleRepository.findByUserIdAndProjectId(userId, projectId).stream()
                    .flatMap(userRole -> userRole.getRole().getPermissions().stream())
                    .forEach(permissions::add);
        }

        return permissions.stream()
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Set<String>> getUserResourcePermissions(String userId) {
        Map<String, Set<String>> resourcePermissions = new HashMap<>();

        // Get all user roles
        List<Permission> allUserPermissions = permissionRepository.findAllByUserId(userId);

        // Group them by resource
        Map<String, List<Permission>> permissionsByResource = allUserPermissions.stream()
                .collect(Collectors.groupingBy(Permission::getResource));

        // Convert to the desired format
        permissionsByResource.forEach((resource, perms) -> {
            Set<String> permissions = perms.stream()
                    .map(Permission::getName)
                    .collect(Collectors.toSet());
            resourcePermissions.put(resource, permissions);
        });

        return resourcePermissions;
    }
}