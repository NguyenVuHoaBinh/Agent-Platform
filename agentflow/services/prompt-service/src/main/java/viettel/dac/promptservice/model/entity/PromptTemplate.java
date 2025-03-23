package viettel.dac.promptservice.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.annotation.CreatedBy;
import viettel.dac.promptservice.model.enums.VersionStatus;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Prompt template entity with enhanced validation and business logic
 */
@Entity
@Table(name = "prompt_templates", indexes = {
        @Index(name = "idx_template_project", columnList = "project_id"),
        @Index(name = "idx_template_category", columnList = "category"),
        @Index(name = "idx_template_created_by", columnList = "created_by"),
        @Index(name = "idx_template_updated_at", columnList = "updated_at"),
        @Index(name = "idx_template_category_updated", columnList = "category, updated_at DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Cacheable("promptTemplates")
public class PromptTemplate extends BaseEntity {

    @NotBlank(message = "Template name is required")
    @Size(min = 3, max = 255, message = "Template name must be between 3 and 255 characters")
    @Pattern(regexp = "^[^<>\"'&;]*$", message = "Template name contains invalid characters")
    @Column(nullable = false)
    private String name;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    @Column(length = 1000)
    private String description;

    @NotBlank(message = "Project ID is required")
    @Column(name = "project_id", nullable = false)
    private String projectId;

    @CreatedBy
    @Column(name = "created_by", nullable = false, updatable = false)
    private String createdBy;

    @Size(max = 50, message = "Category cannot exceed 50 characters")
    @Column(name = "category")
    private String category;

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<PromptVersion> versions = new HashSet<>();

    /**
     * Add a version to this template
     */
    public void addVersion(PromptVersion version) {
        versions.add(version);
        version.setTemplate(this);
    }

    /**
     * Remove a version from this template
     */
    public void removeVersion(PromptVersion version) {
        versions.remove(version);
        version.setTemplate(null);
    }

    /**
     * Get the latest published version of this template, if any exists
     *
     * @return Optional containing the latest published version, or empty if none exists
     */
    public Optional<PromptVersion> getLatestPublishedVersion() {
        return versions.stream()
                .filter(v -> v.getStatus() == VersionStatus.PUBLISHED)
                .max((v1, v2) -> v1.getCreatedAt().compareTo(v2.getCreatedAt()));
    }

    /**
     * Check if this template has any published versions
     *
     * @return true if at least one published version exists
     */
    public boolean hasPublishedVersion() {
        return versions.stream()
                .anyMatch(v -> v.getStatus() == VersionStatus.PUBLISHED);
    }

    /**
     * Create a new draft version based on an existing version
     *
     * @param sourceVersion The source version to copy content from
     * @param createdBy The ID of the user creating the version
     * @return The new draft version
     */
    public PromptVersion createDraftFromVersion(PromptVersion sourceVersion, String createdBy) {
        // Calculate new version number (increment minor version)
        String newVersionNumber = sourceVersion.incrementMinorVersion();

        PromptVersion newVersion = PromptVersion.builder()
                .template(this)
                .versionNumber(newVersionNumber)
                .content(sourceVersion.getContent())
                .createdBy(createdBy)
                .status(VersionStatus.DRAFT)
                .parentVersion(sourceVersion)
                .build();

        // Copy parameters from source version
        sourceVersion.getParameters().forEach(param -> {
            PromptParameter newParam = PromptParameter.builder()
                    .version(newVersion)
                    .name(param.getName())
                    .description(param.getDescription())
                    .parameterType(param.getParameterType())
                    .defaultValue(param.getDefaultValue())
                    .required(param.isRequired())
                    .validationPattern(param.getValidationPattern())
                    .build();
            newVersion.addParameter(newParam);
        });

        this.addVersion(newVersion);
        return newVersion;
    }
}