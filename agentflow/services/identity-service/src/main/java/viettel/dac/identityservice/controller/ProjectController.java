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
import viettel.dac.identityservice.dto.ProjectDto;
import viettel.dac.identityservice.dto.UserDto;
import viettel.dac.identityservice.exception.ResourceNotFoundException;
import viettel.dac.identityservice.model.Project;
import viettel.dac.identityservice.model.User;
import viettel.dac.identityservice.service.ProjectService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    @PreAuthorize("hasAuthority('READ_PROJECTS')")
    public ResponseEntity<Page<ProjectDto>> getAllProjects(Pageable pageable) {
        Page<ProjectDto> projects = projectService.getAllProjects(pageable)
                .map(this::convertToDto);
        return ResponseEntity.ok(projects);
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('READ_PROJECTS')")
    public ResponseEntity<Page<ProjectDto>> searchProjects(
            @RequestParam String query, Pageable pageable) {
        Page<ProjectDto> projects = projectService.searchProjects(query, pageable)
                .map(this::convertToDto);
        return ResponseEntity.ok(projects);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('READ_PROJECTS') or @projectService.isUserMemberOfProject(#id, principal.id)")
    public ResponseEntity<ProjectDto> getProjectById(@PathVariable String id) {
        Project project = projectService.getProjectById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));
        return ResponseEntity.ok(convertToDto(project));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('WRITE_PROJECTS')")
    public ResponseEntity<ProjectDto> createProject(
            @Valid @RequestBody Project project,
            @RequestParam String organizationId) {
        Project createdProject = projectService.createProject(project, organizationId);
        return ResponseEntity.status(HttpStatus.CREATED).body(convertToDto(createdProject));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('WRITE_PROJECTS') or @projectService.isUserMemberOfProject(#id, principal.id)")
    public ResponseEntity<ProjectDto> updateProject(
            @PathVariable String id,
            @Valid @RequestBody Project project) {
        Project updatedProject = projectService.updateProject(id, project);
        return ResponseEntity.ok(convertToDto(updatedProject));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('WRITE_PROJECTS')")
    public ResponseEntity<ApiResponse> deleteProject(@PathVariable String id) {
        projectService.deleteProject(id);
        return ResponseEntity.ok(new ApiResponse(true, "Project deleted successfully"));
    }

    @GetMapping("/organization/{organizationId}")
    @PreAuthorize("hasAuthority('READ_PROJECTS') or @organizationService.isUserMemberOfOrganization(#organizationId, principal.id)")
    public ResponseEntity<List<ProjectDto>> getProjectsByOrganization(@PathVariable String organizationId) {
        List<Project> projects = projectService.getProjectsByOrganization(organizationId);
        List<ProjectDto> projectDtos = projects.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(projectDtos);
    }

    @GetMapping("/{id}/members")
    @PreAuthorize("hasAuthority('READ_PROJECTS') or @projectService.isUserMemberOfProject(#id, principal.id)")
    public ResponseEntity<List<UserDto>> getProjectMembers(@PathVariable String id) {
        List<User> members = projectService.getProjectMembers(id);
        List<UserDto> memberDtos = members.stream()
                .map(this::convertUserToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(memberDtos);
    }

    @PostMapping("/{id}/members/{userId}")
    @PreAuthorize("hasAuthority('WRITE_PROJECTS') or @projectService.isUserMemberOfProject(#id, principal.id)")
    public ResponseEntity<ProjectDto> addMemberToProject(
            @PathVariable String id,
            @PathVariable String userId) {
        Project project = projectService.addUserToProject(id, userId);
        return ResponseEntity.ok(convertToDto(project));
    }

    @DeleteMapping("/{id}/members/{userId}")
    @PreAuthorize("hasAuthority('WRITE_PROJECTS') or @projectService.isUserMemberOfProject(#id, principal.id)")
    public ResponseEntity<ProjectDto> removeMemberFromProject(
            @PathVariable String id,
            @PathVariable String userId) {
        Project project = projectService.removeUserFromProject(id, userId);
        return ResponseEntity.ok(convertToDto(project));
    }

    // Helper methods for DTO conversion
    private ProjectDto convertToDto(Project project) {
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