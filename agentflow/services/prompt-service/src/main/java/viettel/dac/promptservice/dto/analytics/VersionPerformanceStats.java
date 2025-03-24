package viettel.dac.promptservice.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Performance statistics for a specific prompt version
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VersionPerformanceStats {

    /**
     * Version ID
     */
    private String versionId;

    /**
     * Version number
     */
    private String versionNumber;

    /**
     * Template ID
     */
    private String templateId;

    /**
     * Template name
     */
    private String templateName;

    /**
     * Total number of executions
     */
    private Long totalExecutions;

    /**
     * Success rate (percentage)
     */
    private Double successRate;

    /**
     * Average tokens per execution
     */
    private Double averageTokens;

    /**
     * Average input tokens per execution
     */
    private Double averageInputTokens;

    /**
     * Average output tokens per execution
     */
    private Double averageOutputTokens;

    /**
     * Average response time in milliseconds
     */
    private Double averageResponseTime;

    /**
     * Average cost per execution
     */
    private BigDecimal averageCost;

    /**
     * Total cost across all executions
     */
    private BigDecimal totalCost;

    /**
     * Performance by model
     */
    private List<ModelPerformance> performanceByModel;

    /**
     * Comparison with previous version (if applicable)
     */
    private VersionComparison comparisonWithPrevious;

    /**
     * Model-specific performance data
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModelPerformance {
        private String modelId;
        private String providerId;
        private Long executions;
        private Double successRate;
        private Double averageTokens;
        private Double averageResponseTime;
        private BigDecimal averageCost;
    }

    /**
     * Comparison between versions
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VersionComparison {
        private String previousVersionId;
        private String previousVersionNumber;
        private Double tokensDiff;           // Percentage change
        private Double responseTimeDiff;     // Percentage change
        private Double costDiff;             // Percentage change
        private Double successRateDiff;      // Percentage points change
    }
}