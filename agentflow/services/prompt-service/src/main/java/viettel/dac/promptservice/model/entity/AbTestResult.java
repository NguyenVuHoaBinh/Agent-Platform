package viettel.dac.promptservice.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import viettel.dac.promptservice.model.converter.JsonAttributeConverter;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Entity for A/B test results
 */
@Entity
@Table(name = "ab_test_results", indexes = {
        @Index(name = "idx_result_test", columnList = "test_id"),
        @Index(name = "idx_result_version", columnList = "version_id"),
        @Index(name = "idx_result_control", columnList = "is_control")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class AbTestResult extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_id", nullable = false)
    private AbTest test;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "version_id", nullable = false)
    private PromptVersion version;

    @NotNull(message = "Control flag is required")
    @Column(name = "is_control", nullable = false)
    private Boolean controlVersion;

    @Min(value = 1, message = "Sample count must be at least 1")
    @Column(name = "sample_count", nullable = false)
    private Integer sampleCount;

    @Min(value = 0, message = "Success count must be non-negative")
    @Column(name = "success_count", nullable = false)
    private Integer successCount;

    @Column(name = "success_rate")
    private Double successRate;

    @Column(name = "avg_response_time")
    private Double averageResponseTime;

    @Column(name = "avg_tokens")
    private Double averageTokens;

    @Column(name = "avg_cost", precision = 10, scale = 6)
    private BigDecimal averageCost;

    @Column(name = "total_cost", precision = 10, scale = 6)
    private BigDecimal totalCost;

    @Column(name = "p_value")
    private Double pValue;

    @Column(name = "confidence_level")
    private Double confidenceLevel;

    @Convert(converter = JsonAttributeConverter.class)
    @Column(name = "metric_values", columnDefinition = "TEXT")
    private Map<String, Object> metricValues;

    @Convert(converter = JsonAttributeConverter.class)
    @Column(name = "execution_ids", columnDefinition = "TEXT")
    private Map<String, Object> executionIds;

    /**
     * Calculate success rate based on success and sample counts
     */
    public void calculateSuccessRate() {
        if (sampleCount != null && sampleCount > 0) {
            this.successRate = (double) successCount / sampleCount * 100;
        } else {
            this.successRate = 0.0;
        }
    }

    /**
     * Update sample count and recalculate success rate
     *
     * @param additionalSamples Number of additional samples
     * @param additionalSuccesses Number of additional successes
     */
    public void updateCounts(int additionalSamples, int additionalSuccesses) {
        this.sampleCount += additionalSamples;
        this.successCount += additionalSuccesses;
        calculateSuccessRate();
    }

    /**
     * Check if this is the control version
     *
     * @return True if this is the control version
     */
    public boolean isControlVersion() {
        return Boolean.TRUE.equals(this.controlVersion);
    }

    /**
     * Calculate whether this result is significantly better than the comparison
     *
     * @param comparison The result to compare against
     * @return True if this result is significantly better at the given confidence level
     */
    public boolean isSignificantlyBetter(AbTestResult comparison) {
        if (this.pValue == null || this.test.getConfidenceThreshold() == null) {
            return false;
        }

        return this.pValue < (1.0 - this.test.getConfidenceThreshold() / 100.0) &&
                this.successRate > comparison.getSuccessRate();
    }
}