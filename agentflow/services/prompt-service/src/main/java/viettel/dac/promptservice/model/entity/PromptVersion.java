package viettel.dac.promptservice.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.annotation.CreatedBy;
import viettel.dac.promptservice.model.enums.VersionStatus;

import java.util.*;
import java.util.regex.Matcher;

/**
 * Prompt version entity with semantic versioning support
 */
@Entity
@Table(name = "prompt_versions", indexes = {
        @Index(name = "idx_version_template", columnList = "template_id"),
        @Index(name = "idx_version_status", columnList = "status"),
        @Index(name = "idx_version_created_at", columnList = "created_at"),
        @Index(name = "idx_version_created_by", columnList = "created_by"),
        @Index(name = "idx_version_status_created", columnList = "status, created_at DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Cacheable("promptVersions")
public class PromptVersion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private PromptTemplate template;

    @NotBlank(message = "Version number is required")
    @Pattern(regexp = "^\\d+\\.\\d+\\.\\d+$", message = "Version number must follow semver format (major.minor.patch)")
    @Column(name = "version_number", nullable = false)
    private String versionNumber;

    @NotBlank(message = "Content is required")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "system_prompt", columnDefinition = "TEXT")
    private String systemPrompt;

    @CreatedBy
    @Column(name = "created_by", nullable = false, updatable = false)
    private String createdBy;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VersionStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_version_id")
    private PromptVersion parentVersion;

    @OneToMany(mappedBy = "version", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<PromptParameter> parameters = new HashSet<>();

    @OneToMany(mappedBy = "version", cascade = CascadeType.ALL)
    @Builder.Default
    private List<PromptExecution> executions = new ArrayList<>();

    /**
     * Add a parameter to this version
     */
    public void addParameter(PromptParameter parameter) {
        parameters.add(parameter);
        parameter.setVersion(this);
    }

    /**
     * Remove a parameter from this version
     */
    public void removeParameter(PromptParameter parameter) {
        parameters.remove(parameter);
        parameter.setVersion(null);
    }

    /**
     * Get major version number
     */
    public int getMajorVersion() {
        String[] parts = versionNumber.split("\\.");
        return Integer.parseInt(parts[0]);
    }

    /**
     * Get minor version number
     */
    public int getMinorVersion() {
        String[] parts = versionNumber.split("\\.");
        return Integer.parseInt(parts[1]);
    }

    /**
     * Get patch version number
     */
    public int getPatchVersion() {
        String[] parts = versionNumber.split("\\.");
        return Integer.parseInt(parts[2]);
    }

    /**
     * Increment major version (resets minor and patch to 0)
     *
     * @return New version number string
     */
    public String incrementMajorVersion() {
        return (getMajorVersion() + 1) + ".0.0";
    }

    /**
     * Increment minor version (resets patch to 0)
     *
     * @return New version number string
     */
    public String incrementMinorVersion() {
        return getMajorVersion() + "." + (getMinorVersion() + 1) + ".0";
    }

    /**
     * Increment patch version
     *
     * @return New version number string
     */
    public String incrementPatchVersion() {
        return getMajorVersion() + "." + getMinorVersion() + "." + (getPatchVersion() + 1);
    }

    /**
     * Compare version numbers according to semantic versioning rules
     *
     * @param other Version to compare with
     * @return negative if this version is less than other, 0 if equal, positive if greater
     */
    public int compareVersion(PromptVersion other) {
        if (getMajorVersion() != other.getMajorVersion()) {
            return getMajorVersion() - other.getMajorVersion();
        }
        if (getMinorVersion() != other.getMinorVersion()) {
            return getMinorVersion() - other.getMinorVersion();
        }
        return getPatchVersion() - other.getPatchVersion();
    }

    /**
     * Check if this version can transition to the given status
     *
     * @param newStatus Status to transition to
     * @return true if transition is allowed
     */
    public boolean canTransitionTo(VersionStatus newStatus) {
        if (status == newStatus) {
            return true;
        }

        switch (status) {
            case DRAFT:
                return newStatus == VersionStatus.REVIEW;
            case REVIEW:
                return newStatus == VersionStatus.APPROVED || newStatus == VersionStatus.DRAFT;
            case APPROVED:
                return newStatus == VersionStatus.PUBLISHED || newStatus == VersionStatus.REVIEW;
            case PUBLISHED:
                return newStatus == VersionStatus.DEPRECATED;
            case DEPRECATED:
                return newStatus == VersionStatus.ARCHIVED;
            case ARCHIVED:
                return false;
            default:
                return false;
        }
    }

    /**
     * Apply parameter substitution to content
     *
     * @param paramValues Map of parameter values to substitute
     * @return Content with parameters substituted
     * @throws IllegalArgumentException if required parameters are missing
     */
    public String applyParameters(Map<String, Object> paramValues) {
        String result = content;
        Set<String> missingRequired = new HashSet<>();

        // Check for required parameters
        for (PromptParameter param : parameters) {
            if (param.isRequired() &&
                    (paramValues == null ||
                            !paramValues.containsKey(param.getName()) ||
                            paramValues.get(param.getName()) == null)) {
                missingRequired.add(param.getName());
            }
        }

        if (!missingRequired.isEmpty()) {
            throw new IllegalArgumentException("Missing required parameters: " + String.join(", ", missingRequired));
        }

        // Apply substitution
        if (paramValues != null) {
            for (Map.Entry<String, Object> entry : paramValues.entrySet()) {
                String placeholder = "{{" + entry.getKey() + "}}";
                if (result.contains(placeholder)) {
                    result = result.replace(placeholder, String.valueOf(entry.getValue()));
                }
            }
        }

        return result;
    }

    /**
     * Extract parameter placeholders from content
     *
     * @return Set of parameter names found in content
     */
    public Set<String> extractParametersFromContent() {
        Set<String> parameters = new HashSet<>();
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\{\\{([^}]+)\\}\\}");
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            parameters.add(matcher.group(1));
        }

        return parameters;
    }
}