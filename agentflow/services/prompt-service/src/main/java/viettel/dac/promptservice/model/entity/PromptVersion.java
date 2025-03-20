package viettel.dac.promptservice.model.entity;


import jakarta.persistence.*;
import lombok.*;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.annotation.CreatedBy;
import viettel.dac.promptservice.model.enums.VersionStatus;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Prompt version entity
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
@Builder
@Cacheable("promptVersions")
public class PromptVersion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private PromptTemplate template;

    @Column(name = "version_number", nullable = false)
    private String versionNumber;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @CreatedBy
    @Column(name = "created_by", nullable = false, updatable = false)
    private String createdBy;

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
}