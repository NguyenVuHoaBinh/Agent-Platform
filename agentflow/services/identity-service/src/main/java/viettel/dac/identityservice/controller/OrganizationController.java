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
import viettel.dac.identityservice.dto.request.OrganizationRequest;
import viettel.dac.identityservice.exception.ResourceNotFoundException;
import viettel.dac.identityservice.model.Organization;
import viettel.dac.identityservice.model.Project;
import viettel.dac.identityservice.model.User;
import viettel.dac.identityservice.service.EntityDtoMapper;
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
    private final EntityDtoMapper mapper;

    @GetMapping
    @PreAuthorize("hasAuthority('READ_ORGANIZATIONS')")
    public ResponseEntity<Page<OrganizationDto>> getAllOrganizations(Pageable pageable) {
        Page<OrganizationDto> organizations = organizationService.getAllOrganizations(pageable)
                .map(mapper::toDto);
        return ResponseEntity.ok(organizations);
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('READ_ORGANIZATIONS')")
    public ResponseEntity<Page<OrganizationDto>> searchOrganizations(
            @RequestParam String query, Pageable pageable) {
        Page<OrganizationDto> organizations = organizationService.searchOrganizations(query, pageable)
                .map(mapper::toDto);
        return ResponseEntity.ok(organizations);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('READ_ORGANIZATIONS') or @organizationService.isUserMemberOfOrganization(#id, principal.id)")
    public ResponseEntity<OrganizationDto> getOrganizationById(@PathVariable String id) {
        Organization organization = organizationService.getOrganizationById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + id));
        return ResponseEntity.ok(mapper.toDto(organization));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('WRITE_ORGANIZATIONS')")
    public ResponseEntity<OrganizationDto> createOrganization(@Valid @RequestBody OrganizationRequest request) {
        // Convert the request DTO to an entity
        Organization organization = mapper.toEntity(request);

        // Save the entity
        Organization createdOrganization = organizationService.createOrganization(organization);

        // Convert the saved entity back to a DTO
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toDto(createdOrganization));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('WRITE_ORGANIZATIONS') or @organizationService.isUserMemberOfOrganization(#id, principal.id)")
    public ResponseEntity<OrganizationDto> updateOrganization(
            @PathVariable String id,
            @Valid @RequestBody OrganizationRequest request) {

        // Get the existing organization
        Organization organization = organizationService.getOrganizationById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + id));

        // Update the entity from the request
        mapper.updateOrganizationFromRequest(organization, request);

        // Save the updated entity
        Organization updatedOrganization = organizationService.updateOrganization(id, organization);

        // Return the updated entity as DTO
        return ResponseEntity.ok(mapper.toDto(updatedOrganization));
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
                .map(mapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(memberDtos);
    }

    @PostMapping("/{id}/members/{userId}")
    @PreAuthorize("hasAuthority('WRITE_ORGANIZATIONS') or @organizationService.isUserMemberOfOrganization(#id, principal.id)")
    public ResponseEntity<OrganizationDto> addMemberToOrganization(
            @PathVariable String id,
            @PathVariable String userId) {
        Organization organization = organizationService.addUserToOrganization(id, userId);
        return ResponseEntity.ok(mapper.toDto(organization));
    }

    @DeleteMapping("/{id}/members/{userId}")
    @PreAuthorize("hasAuthority('WRITE_ORGANIZATIONS') or @organizationService.isUserMemberOfOrganization(#id, principal.id)")
    public ResponseEntity<OrganizationDto> removeMemberFromOrganization(
            @PathVariable String id,
            @PathVariable String userId) {
        Organization organization = organizationService.removeUserFromOrganization(id, userId);
        return ResponseEntity.ok(mapper.toDto(organization));
    }

    @GetMapping("/{id}/projects")
    @PreAuthorize("hasAuthority('READ_ORGANIZATIONS') or @organizationService.isUserMemberOfOrganization(#id, principal.id)")
    public ResponseEntity<List<ProjectDto>> getOrganizationProjects(@PathVariable String id) {
        List<Project> projects = projectService.getProjectsByOrganization(id);
        List<ProjectDto> projectDtos = projects.stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(projectDtos);
    }
}