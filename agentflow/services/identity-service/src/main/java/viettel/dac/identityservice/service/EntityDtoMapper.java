package viettel.dac.identityservice.service;

import org.springframework.stereotype.Service;
import viettel.dac.identityservice.dto.OrganizationDto;
import viettel.dac.identityservice.dto.ProjectDto;
import viettel.dac.identityservice.dto.UserDto;
import viettel.dac.identityservice.dto.request.OrganizationRequest;
import viettel.dac.identityservice.dto.request.ProjectRequest;
import viettel.dac.identityservice.dto.request.UserRequest;
import viettel.dac.identityservice.model.Organization;
import viettel.dac.identityservice.model.Project;
import viettel.dac.identityservice.model.Role;
import viettel.dac.identityservice.model.User;

import java.util.stream.Collectors;

/**
 * Service for mapping between entities and DTOs
 */
@Service
public class EntityDtoMapper {

    /**
     * Convert Organization entity to DTO
     */
    public OrganizationDto toDto(Organization organization) {
        return OrganizationDto.builder()
                .id(organization.getId())
                .name(organization.getName())
                .description(organization.getDescription())
                .createdAt(organization.getCreatedAt())
                .updatedAt(organization.getUpdatedAt())
                .build();
    }

    /**
     * Convert OrganizationRequest to Organization entity
     * Note: This doesn't set the ID, createdAt or updatedAt fields
     */
    public Organization toEntity(OrganizationRequest request) {
        return Organization.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
    }

    /**
     * Update an existing Organization entity from OrganizationRequest
     */
    public void updateOrganizationFromRequest(Organization organization, OrganizationRequest request) {
        organization.setName(request.getName());
        organization.setDescription(request.getDescription());
    }

    /**
     * Convert Project entity to DTO
     */
    public ProjectDto toDto(Project project) {
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

    /**
     * Convert ProjectRequest to Project entity
     * Note: This doesn't set the ID, organization, createdAt or updatedAt fields
     */
    public Project toEntity(ProjectRequest request) {
        return Project.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
    }

    /**
     * Update an existing Project entity from ProjectRequest
     * Note: This doesn't update the organization field
     */
    public void updateProjectFromRequest(Project project, ProjectRequest request) {
        project.setName(request.getName());
        project.setDescription(request.getDescription());
    }

    /**
     * Convert User entity to DTO
     */
    public UserDto toDto(User user) {
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

    /**
     * Convert UserRequest to User entity
     * Note: This doesn't set the ID, passwordHash, roles, organizations, projects,
     * createdAt or updatedAt fields
     */
    public User toEntity(UserRequest request) {
        return User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .enabled(request.isEnabled())
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .build();
    }

    /**
     * Update an existing User entity from UserRequest
     * Note: This doesn't update the passwordHash, roles, organizations, or projects fields
     */
    public void updateUserFromRequest(User user, UserRequest request) {
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEnabled(request.isEnabled());
    }
}