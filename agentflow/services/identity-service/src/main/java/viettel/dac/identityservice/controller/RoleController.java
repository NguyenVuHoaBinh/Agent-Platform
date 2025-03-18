package viettel.dac.identityservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import viettel.dac.identityservice.dto.ApiResponse;
import viettel.dac.identityservice.dto.PermissionDto;
import viettel.dac.identityservice.dto.RoleDto;
import viettel.dac.identityservice.dto.UserDto;
import viettel.dac.identityservice.exception.ResourceNotFoundException;
import viettel.dac.identityservice.model.Permission;
import viettel.dac.identityservice.model.Role;
import viettel.dac.identityservice.model.User;
import viettel.dac.identityservice.service.RoleService;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @PreAuthorize("hasAuthority('READ_USERS')")
    public ResponseEntity<List<RoleDto>> getAllRoles() {
        List<Role> roles = roleService.getAllRoles();
        List<RoleDto> roleDtos = roles.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(roleDtos);
    }

    @GetMapping("/with-permissions")
    @PreAuthorize("hasAuthority('READ_USERS')")
    public ResponseEntity<List<RoleDto>> getAllRolesWithPermissions() {
        List<Role> roles = roleService.getAllRolesWithPermissions();
        List<RoleDto> roleDtos = roles.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(roleDtos);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('READ_USERS')")
    public ResponseEntity<RoleDto> getRoleById(@PathVariable String id) {
        Role role = roleService.getRoleByIdWithPermissions(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + id));
        return ResponseEntity.ok(convertToDto(role));
    }

    @GetMapping("/name/{name}")
    @PreAuthorize("hasAuthority('READ_USERS')")
    public ResponseEntity<RoleDto> getRoleByName(@PathVariable String name) {
        Role role = roleService.getRoleByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with name: " + name));
        return ResponseEntity.ok(convertToDto(role));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('WRITE_USERS')")
    public ResponseEntity<RoleDto> createRole(@Valid @RequestBody Role role) {
        Role createdRole = roleService.createRole(role);
        return ResponseEntity.status(HttpStatus.CREATED).body(convertToDto(createdRole));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('WRITE_USERS')")
    public ResponseEntity<RoleDto> updateRole(
            @PathVariable String id,
            @Valid @RequestBody Role role) {
        Role updatedRole = roleService.updateRole(id, role);
        return ResponseEntity.ok(convertToDto(updatedRole));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('WRITE_USERS')")
    public ResponseEntity<ApiResponse> deleteRole(@PathVariable String id) {
        roleService.deleteRole(id);
        return ResponseEntity.ok(new ApiResponse(true, "Role deleted successfully"));
    }

    @GetMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('READ_USERS')")
    public ResponseEntity<Set<PermissionDto>> getRolePermissions(@PathVariable String id) {
        Set<Permission> permissions = roleService.getRolePermissions(id);
        Set<PermissionDto> permissionDtos = permissions.stream()
                .map(this::convertPermissionToDto)
                .collect(Collectors.toSet());
        return ResponseEntity.ok(permissionDtos);
    }

    @PostMapping("/{id}/permissions/{permissionId}")
    @PreAuthorize("hasAuthority('WRITE_USERS')")
    public ResponseEntity<RoleDto> addPermissionToRole(
            @PathVariable String id,
            @PathVariable String permissionId) {
        Role role = roleService.addPermissionToRole(id, permissionId);
        return ResponseEntity.ok(convertToDto(role));
    }

    @PostMapping("/{id}/permissions/name/{permissionName}")
    @PreAuthorize("hasAuthority('WRITE_USERS')")
    public ResponseEntity<RoleDto> addPermissionToRoleByName(
            @PathVariable String id,
            @PathVariable String permissionName) {
        Role role = roleService.addPermissionToRoleByName(id, permissionName);
        return ResponseEntity.ok(convertToDto(role));
    }

    @DeleteMapping("/{id}/permissions/{permissionId}")
    @PreAuthorize("hasAuthority('WRITE_USERS')")
    public ResponseEntity<RoleDto> removePermissionFromRole(
            @PathVariable String id,
            @PathVariable String permissionId) {
        Role role = roleService.removePermissionFromRole(id, permissionId);
        return ResponseEntity.ok(convertToDto(role));
    }

    @DeleteMapping("/{id}/permissions/name/{permissionName}")
    @PreAuthorize("hasAuthority('WRITE_USERS')")
    public ResponseEntity<RoleDto> removePermissionFromRoleByName(
            @PathVariable String id,
            @PathVariable String permissionName) {
        Role role = roleService.removePermissionFromRoleByName(id, permissionName);
        return ResponseEntity.ok(convertToDto(role));
    }

    @GetMapping("/name/{roleName}/users")
    @PreAuthorize("hasAuthority('READ_USERS')")
    public ResponseEntity<List<UserDto>> getUsersByRole(@PathVariable String roleName) {
        List<User> users = roleService.getUsersByRole(roleName);
        List<UserDto> userDtos = users.stream()
                .map(this::convertUserToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(userDtos);
    }

    @PostMapping("/{roleId}/users/{userId}")
    @PreAuthorize("hasAuthority('WRITE_USERS')")
    public ResponseEntity<ApiResponse> assignRoleToUser(
            @PathVariable String roleId,
            @PathVariable String userId) {
        roleService.assignRoleToUser(roleId, userId);
        return ResponseEntity.ok(new ApiResponse(true, "Role assigned to user successfully"));
    }

    @DeleteMapping("/{roleId}/users/{userId}")
    @PreAuthorize("hasAuthority('WRITE_USERS')")
    public ResponseEntity<ApiResponse> removeRoleFromUser(
            @PathVariable String roleId,
            @PathVariable String userId) {
        roleService.removeRoleFromUser(roleId, userId);
        return ResponseEntity.ok(new ApiResponse(true, "Role removed from user successfully"));
    }

    // Helper methods for DTO conversion
    private RoleDto convertToDto(Role role) {
        return RoleDto.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .permissions(role.getPermissions().stream()
                        .map(permission -> permission.getName())
                        .collect(Collectors.toSet()))
                .build();
    }

    private PermissionDto convertPermissionToDto(Permission permission) {
        return PermissionDto.builder()
                .id(permission.getId())
                .name(permission.getName())
                .description(permission.getDescription())
                .build();
    }

    private UserDto convertUserToDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .enabled(user.isEnabled())
                .roles(user.getRoles().stream()
                        .map(role -> role.getName())
                        .collect(Collectors.toSet()))
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}