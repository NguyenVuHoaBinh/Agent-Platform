package viettel.dac.promptservice.model.entity;


import jakarta.persistence.*;
import lombok.*;
import org.springframework.cache.annotation.Cacheable;
import viettel.dac.promptservice.model.enums.ParameterType;

/**
 * Prompt parameter entity
 */
@Entity
@Table(name = "prompt_parameters", indexes = {
        @Index(name = "idx_parameter_version", columnList = "version_id"),
        @Index(name = "idx_parameter_type", columnList = "parameter_type"),
        @Index(name = "idx_parameter_required", columnList = "required")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Cacheable("promptParameters")
public class PromptParameter extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "version_id", nullable = false)
    private PromptVersion version;

    @Column(nullable = false)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "parameter_type", nullable = false)
    private ParameterType parameterType;

    @Column(name = "default_value", columnDefinition = "TEXT")
    private String defaultValue;

    @Column(nullable = false)
    private boolean required;

    @Column(name = "validation_pattern")
    private String validationPattern;
}
