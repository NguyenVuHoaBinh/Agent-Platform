package viettel.dac.promptservice.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.annotation.CreatedBy;

import java.util.HashSet;
import java.util.Set;

/**
 * Prompt template entity
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
@Builder
@Cacheable("promptTemplates")
public class PromptTemplate extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(name = "project_id", nullable = false)
    private String projectId;

    @CreatedBy
    @Column(name = "created_by", nullable = false, updatable = false)
    private String createdBy;

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
}