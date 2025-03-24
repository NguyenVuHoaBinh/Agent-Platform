package viettel.dac.promptservice.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedBy;
import viettel.dac.promptservice.model.converter.JsonAttributeConverter;
import viettel.dac.promptservice.model.enums.TestStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Entity for A/B test configurations
 */
@Entity
@Table(name = "ab_tests", indexes = {
        @Index(name = "idx_test_created_by", columnList = "created_by"),
        @Index(name = "idx_test_status", columnList = "status"),
        @Index(name = "idx_test_name", columnList = "name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class AbTest extends BaseEntity {

    @NotBlank(message = "Test name is required")
    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @NotNull(message = "Test status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TestStatus status;

    @CreatedBy
    @Column(name = "created_by", nullable = false, updatable = false)
    private String createdBy;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "control_version_id", nullable = false)
    private PromptVersion controlVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_version_id", nullable = false)
    private PromptVersion variantVersion;

    @Min(value = 1, message = "Sample size must be at least 1")
    @Column(name = "sample_size", nullable = false)
    private Integer sampleSize;

    @Min(value = 0, message = "Confidence level must be non-negative")
    @Column(name = "confidence_threshold", nullable = false)
    private Double confidenceThreshold;

    @Column(name = "evaluation_metric", nullable = false)
    private String evaluationMetric;

    @Convert(converter = JsonAttributeConverter.class)
    @Column(name = "test_parameters", columnDefinition = "TEXT")
    private Map<String, Object> testParameters;

    @Column(name = "success_criteria")
    private String successCriteria;

    @Column(name = "provider_id")
    private String providerId;

    @Column(name = "model_id")
    private String modelId;

    @OneToMany(mappedBy = "test", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AbTestResult> results = new ArrayList<>();

    /**
     * Add a result to this test
     */
    public void addResult(AbTestResult result) {
        results.add(result);
        result.setTest(this);
    }

    /**
     * Remove a result from this test
     */
    public void removeResult(AbTestResult result) {
        results.remove(result);
        result.setTest(null);
    }

    /**
     * Get the total number of executions for both control and variant
     */
    public int getTotalExecutions() {
        return results.stream()
                .mapToInt(AbTestResult::getSampleCount)
                .sum();
    }

    /**
     * Get the number of executions for control version
     */
    public int getControlExecutions() {
        return results.stream()
                .filter(r -> r.isControlVersion())
                .mapToInt(AbTestResult::getSampleCount)
                .sum();
    }

    /**
     * Get the number of executions for variant version
     */
    public int getVariantExecutions() {
        return results.stream()
                .filter(r -> !r.isControlVersion())
                .mapToInt(AbTestResult::getSampleCount)
                .sum();
    }

    /**
     * Check if test is complete
     */
    public boolean isComplete() {
        return status == TestStatus.COMPLETED || status == TestStatus.CANCELLED;
    }

    /**
     * Check if test is active
     */
    public boolean isActive() {
        return status == TestStatus.RUNNING;
    }

    /**
     * Check if test is ready to start
     */
    public boolean isReadyToStart() {
        return status == TestStatus.CREATED;
    }
}