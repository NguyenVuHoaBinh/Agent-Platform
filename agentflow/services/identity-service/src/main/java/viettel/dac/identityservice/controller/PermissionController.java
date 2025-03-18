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
import viettel.dac.identityservice.exception.ResourceNotFoundException;
import viettel.dac.identityservice.model.Permission;
import viettel.dac.identityservice.model.Role;
import viettel.dac.identityservice.service.PermissionService;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping
    @PreAuthorize("hasAuthority('READ_USERS')")
    public ResponseEntity<List<PermissionDto>> getAllPermissions() {
        List<Permission> permissions = permissionService.getAllPermissions();
        List<PermissionDto> permissionDtos = permissions.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(permissionDtos);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('READ_USERS')")
    public ResponseEntity<PermissionDto> getPermissionById(@PathVariable String id) {
        Permission permission = permissionService.getPermissionById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found with id: " + id));
        return ResponseEntity.ok(convertToDto(permission));
    }

    @GetMapping("/name/{name}")
    @PreAuthorize("hasAuthority('READ_USERS')")
    public ResponseEntity<PermissionDto> getPermissionByName(@PathVariable String name) {
        Permission permission = permissionService.getPermissionByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found with name: " + name));
        return ResponseEntity.ok(convertToDto(permission));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('WRITE_USERS')")
    public ResponseEntity<PermissionDto> createPermission(@Valid @RequestBody Permission permission) {
        Permission createdPermission = permissionService.createPermission(permission);
        return ResponseEntity.status(HttpStatus.CREATED).body(convertToDto(createdPermission));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('WRITE_USERS')")
    public ResponseEntity<PermissionDto> updatePermission(
            @PathVariable String id,
            @Valid @RequestBody Permission permission) {
        Permission updatedPermission = permissionService.updatePermission(id, permission);
        return ResponseEntity.ok(convertToDto(updatedPermission));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('WRITE_USERS')")
    public ResponseEntity<ApiResponse> deletePermission(@PathVariable String id) {
        permissionService.deletePermission(id);
        return ResponseEntity.ok(new ApiResponse(true, "Permission deleted successfully"));
    }

    @GetMapping("/role/{roleId}")
    @PreAuthorize("hasAuthority('READ_USERS')")
    public ResponseEntity<Set<PermissionDto>> getPermissionsByRoleId(@PathVariable String roleId) {
        Set<Permission> permissions = permissionService.getPermissionsByRoleId(roleId);
        Set<PermissionDto> permissionDtos = permissions.stream()
                .map(this::convertToDto)
                .collect(Collectors.toSet());
        return ResponseEntity.ok(permissionDtos);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAuthority('READ_USERS')")
    public ResponseEntity<Set<PermissionDto>> getPermissionsByUserId(@PathVariable String userId) {
        Set<Permission> permissions = permissionService.getPermissionsByUserId(userId);
        Set<PermissionDto> permissionDtos = permissions.stream()
                .map(this::convertToDto)
                .collect(Collectors.toSet());
        return ResponseEntity.ok(permissionDtos);
    }

    @GetMapping("/username/{username}")
    @PreAuthorize("hasAuthority('READ_USERS')")
    public ResponseEntity<List<PermissionDto>> getPermissionsByUsername(@PathVariable String username) {
        List<Permission> permissions = permissionService.getPermissionsByUsername(username);
        List<PermissionDto> permissionDtos = permissions.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(permissionDtos);
    }

    @GetMapping("/name/{permissionName}/roles")
    @PreAuthorize("hasAuthority('READ_USERS')")
    public ResponseEntity<List<RoleDto>> getRolesByPermission(@PathVariable String permissionName) {
        List<Role> roles = permissionService.getRolesByPermission(permissionName);
        List<RoleDto> roleDtos = roles.stream()
                .map(this::convertRoleToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(roleDtos);
    }

    @PostMapping("/{permissionId}/roles/{roleId}")
    @PreAuthorize("hasAuthority('WRITE_USERS')")
    public ResponseEntity<ApiResponse> assignPermissionToRole(
            @PathVariable String permissionId,
            @PathVariable String roleId) {
        permissionService.assignPermissionToRole(permissionId, roleId);
        return ResponseEntity.ok(new ApiResponse(true, "Permission assigned to role successfully"));
    }

    @DeleteMapping("/{permissionId}/roles/{roleId}")
    @PreAuthorize("hasAuthority('WRITE_USERS')")
    public ResponseEntity<ApiResponse> removePermissionFromRole(
            @PathVariable String permissionId,
            @PathVariable String roleId) {
        permissionService.removePermissionFromRole(permissionId, roleId);
        return ResponseEntity.ok(new ApiResponse(true, "Permission removed from role successfully"));
    }

    @GetMapping("/check/{userId}/{permissionName}")
    @PreAuthorize("hasAuthority('READ_USERS')")
    public ResponseEntity<ApiResponse> checkUserHasPermission(
            @PathVariable String userId,
            @PathVariable String permissionName) {
        boolean hasPermission = permissionService.hasPermission(userId, permissionName);
        return ResponseEntity.ok(new ApiResponse(true,
                hasPermission ? "User has permission" : "User does not have permission",
                hasPermission));
    }

    @PostMapping("/create-crud/{resourceName}")
    @PreAuthorize("hasAuthority('WRITE_USERS')")
    public ResponseEntity<List<PermissionDto>> createCrudPermissions(@PathVariable String resourceName) {
        List<Permission> permissions = permissionService.createCrudPermissions(resourceName);
        List<PermissionDto> permissionDtos = permissions.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.status(HttpStatus.CREATED).body(permissionDtos);
    }

    // Helper methods for DTO conversion
    private PermissionDto convertToDto(Permission permission) {
        return PermissionDto.builder()
                .id(permission.getId())
                .name(permission.getName())
                .description(permission.getDescription())
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
}