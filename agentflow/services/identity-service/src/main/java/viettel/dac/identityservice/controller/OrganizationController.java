package viettel.dac.identityservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import viettel.dac.identityservice.dto.ApiResponse;
import viettel.dac.identityservice.dto.OrganizationDto;
import viettel.dac.identityservice.dto.ProjectDto;
import viettel.dac.identityservice.dto.UserDto;
import viettel.dac.identityservice.exception.ResourceNotFoundException;
import viettel.dac.identityservice.model.Organization;
import viettel.dac.identityservice.model.Project;
import viettel.dac.identityservice.model.User;
import viettel.dac.identityservice.service.OrganizationService;
import viettel.dac.identityservice.service.ProjectService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;
    private final ProjectService projectService;

    @GetMapping
    @PreAuthorize("hasAuthority('READ_ORGANIZATIONS')")
    public ResponseEntity<Page<OrganizationDto>> getAllOrganizations(Pageable pageable) {
        Page<OrganizationDto> organizations = organizationService.getAllOrganizations(pageable)
                .map(this::convertToDto);
        return ResponseEntity.ok(organizations);
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('READ_ORGANIZATIONS')")
    public ResponseEntity<Page<OrganizationDto>> searchOrganizations(
            @RequestParam String query, Pageable pageable) {
        Page<OrganizationDto> organizations = organizationService.searchOrganizations(query, pageable)
                .map(this::convertToDto);
        return ResponseEntity.ok(organizations);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('READ_ORGANIZATIONS') or @organizationService.isUserMemberOfOrganization(#id, principal.id)")
    public ResponseEntity<OrganizationDto> getOrganizationById(@PathVariable String id) {
        Organization organization = organizationService.getOrganizationById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + id));
        return ResponseEntity.ok(convertToDto(organization));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('WRITE_ORGANIZATIONS')")
    public ResponseEntity<OrganizationDto> createOrganization(@Valid @RequestBody Organization organization) {
        Organization createdOrganization = organizationService.createOrganization(organization);
        return ResponseEntity.status(HttpStatus.CREATED).body(convertToDto(createdOrganization));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('WRITE_ORGANIZATIONS') or @organizationService.isUserMemberOfOrganization(#id, principal.id)")
    public ResponseEntity<OrganizationDto> updateOrganization(
            @PathVariable String id,
            @Valid @RequestBody Organization organization) {
        Organization updatedOrganization = organizationService.updateOrganization(id, organization);
        return ResponseEntity.ok(convertToDto(updatedOrganization));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('WRITE_ORGANIZATIONS')")
    public ResponseEntity<ApiResponse> deleteOrganization(@PathVariable String id) {
        organizationService.deleteOrganization(id);
        return ResponseEntity.ok(new ApiResponse(true, "Organization deleted successfully"));
    }

    @GetMapping("/{id}/members")
    @PreAuthorize("hasAuthority('READ_ORGANIZATIONS') or @organizationService.isUserMemberOfOrganization(#id, principal.id)")
    public ResponseEntity<List<UserDto>> getOrganizationMembers(@PathVariable String id) {
        List<User> members = organizationService.getOrganizationMembers(id);
        List<UserDto> memberDtos = members.stream()
                .map(this::convertUserToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(memberDtos);
    }

    @PostMapping("/{id}/members/{userId}")
    @PreAuthorize("hasAuthority('WRITE_ORGANIZATIONS') or @organizationService.isUserMemberOfOrganization(#id, principal.id)")
    public ResponseEntity<OrganizationDto> addMemberToOrganization(
            @PathVariable String id,
            @PathVariable String userId) {
        Organization organization = organizationService.addUserToOrganization(id, userId);
        return ResponseEntity.ok(convertToDto(organization));
    }

    @DeleteMapping("/{id}/members/{userId}")
    @PreAuthorize("hasAuthority('WRITE_ORGANIZATIONS') or @organizationService.isUserMemberOfOrganization(#id, principal.id)")
    public ResponseEntity<OrganizationDto> removeMemberFromOrganization(
            @PathVariable String id,
            @PathVariable String userId) {
        Organization organization = organizationService.removeUserFromOrganization(id, userId);
        return ResponseEntity.ok(convertToDto(organization));
    }

    @GetMapping("/{id}/projects")
    @PreAuthorize("hasAuthority('READ_ORGANIZATIONS') or @organizationService.isUserMemberOfOrganization(#id, principal.id)")
    public ResponseEntity<List<ProjectDto>> getOrganizationProjects(@PathVariable String id) {
        List<Project> projects = projectService.getProjectsByOrganization(id);
        List<ProjectDto> projectDtos = projects.stream()
                .map(this::convertProjectToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(projectDtos);
    }

    // Helper methods for DTO conversion
    private OrganizationDto convertToDto(Organization organization) {
        return OrganizationDto.builder()
                .id(organization.getId())
                .name(organization.getName())
                .description(organization.getDescription())
                .createdAt(organization.getCreatedAt())
                .updatedAt(organization.getUpdatedAt())
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