package viettel.dac.identityservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import viettel.dac.identityservice.dto.*;
import viettel.dac.identityservice.exception.ResourceNotFoundException;
import viettel.dac.identityservice.model.Organization;
import viettel.dac.identityservice.model.Project;
import viettel.dac.identityservice.model.Role;
import viettel.dac.identityservice.model.User;
import viettel.dac.identityservice.service.UserService;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAuthority('READ_USERS')")
    public ResponseEntity<Page<UserDto>> getAllUsers(Pageable pageable) {
        Page<UserDto> users = userService.getAllUsers(pageable)
                .map(this::convertToDto);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('READ_USERS')")
    public ResponseEntity<Page<UserDto>> searchUsers(@RequestParam String query, Pageable pageable) {
        Page<UserDto> users = userService.searchUsers(query, pageable)
                .map(this::convertToDto);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('READ_USERS') or @securityUtils.isCurrentUser(#id)")
    public ResponseEntity<UserDto> getUserById(@PathVariable String id) {
        User user = userService.getUserById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return ResponseEntity.ok(convertToDto(user));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('WRITE_USERS')")
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody User user) {
        User createdUser = userService.createUser(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(convertToDto(createdUser));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('WRITE_USERS') or @securityUtils.isCurrentUser(#id)")
    public ResponseEntity<UserDto> updateUser(@PathVariable String id, @Valid @RequestBody User user) {
        User updatedUser = userService.updateUser(id, user);
        return ResponseEntity.ok(convertToDto(updatedUser));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('WRITE_USERS')")
    public ResponseEntity<ApiResponse> deleteUser(@PathVariable String id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(new ApiResponse(true, "User deleted successfully"));
    }

    @PutMapping("/{id}/password")
    @PreAuthorize("@securityUtils.isCurrentUser(#id)")
    public ResponseEntity<ApiResponse> changePassword(
            @PathVariable String id,
            @Valid @RequestBody PasswordChangeRequest request) {

        // Validate password confirmation
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, "Password confirmation does not match"));
        }

        userService.changePassword(id, request.getCurrentPassword(), request.getNewPassword());
        return ResponseEntity.ok(new ApiResponse(true, "Password changed successfully"));
    }

    @GetMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('READ_USERS') or @securityUtils.isCurrentUser(#id)")
    public ResponseEntity<Set<RoleDto>> getUserRoles(@PathVariable String id) {
        Set<Role> roles = userService.getUserRoles(id);
        Set<RoleDto> roleDtos = roles.stream()
                .map(this::convertRoleToDto)
                .collect(Collectors.toSet());
        return ResponseEntity.ok(roleDtos);
    }

    @PutMapping("/{id}/roles/{roleName}")
    @PreAuthorize("hasAuthority('WRITE_USERS')")
    public ResponseEntity<UserDto> addRoleToUser(@PathVariable String id, @PathVariable String roleName) {
        User user = userService.addRoleToUser(id, roleName);
        return ResponseEntity.ok(convertToDto(user));
    }

    @DeleteMapping("/{id}/roles/{roleName}")
    @PreAuthorize("hasAuthority('WRITE_USERS')")
    public ResponseEntity<UserDto> removeRoleFromUser(@PathVariable String id, @PathVariable String roleName) {
        User user = userService.removeRoleFromUser(id, roleName);
        return ResponseEntity.ok(convertToDto(user));
    }

    @GetMapping("/{id}/organizations")
    @PreAuthorize("hasAuthority('READ_USERS') or @securityUtils.isCurrentUser(#id)")
    public ResponseEntity<List<OrganizationDto>> getUserOrganizations(@PathVariable String id) {
        List<Organization> organizations = userService.getUserOrganizations(id);
        List<OrganizationDto> orgDtos = organizations.stream()
                .map(this::convertOrganizationToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(orgDtos);
    }

    @GetMapping("/{id}/projects")
    @PreAuthorize("hasAuthority('READ_USERS') or @securityUtils.isCurrentUser(#id)")
    public ResponseEntity<List<ProjectDto>> getUserProjects(@PathVariable String id) {
        List<Project> projects = userService.getUserProjects(id);
        List<ProjectDto> projectDtos = projects.stream()
                .map(this::convertProjectToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(projectDtos);
    }

    @GetMapping("/{id}/projects/organization/{organizationId}")
    @PreAuthorize("hasAuthority('READ_USERS') or @securityUtils.isCurrentUser(#id)")
    public ResponseEntity<List<ProjectDto>> getUserProjectsInOrganization(
            @PathVariable String id,
            @PathVariable String organizationId) {
        List<Project> projects = userService.getUserProjectsInOrganization(id, organizationId);
        List<ProjectDto> projectDtos = projects.stream()
                .map(this::convertProjectToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(projectDtos);
    }

    // Helper methods for DTO conversion
    private UserDto convertToDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .enabled(user.isEnabled())
                .roles(user.getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.toSet()))
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private RoleDto convertRoleToDto(Role role) {
        return RoleDto.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .permissions(role.getPermissions().stream()
                        .map(permission -> permission.getName())
                        .collect(Collectors.toSet()))
                .build();
    }

    private OrganizationDto convertOrganizationToDto(Organization organization) {
        return OrganizationDto.builder()
                .id(organization.getId())
                .name(organization.getName())
                .description(organization.getDescription())
                .createdAt(organization.getCreatedAt())
                .updatedAt(organization.getUpdatedAt())
                .build();
    }

    private ProjectDto convertProjectToDto(Project project) {
        return ProjectDto.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .organizationId(project.getOrganization().getId())
                .organizationName(project.getOrganization().getName())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }
}