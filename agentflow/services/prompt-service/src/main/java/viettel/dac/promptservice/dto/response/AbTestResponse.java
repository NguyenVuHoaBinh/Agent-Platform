package viettel.dac.promptservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import viettel.dac.promptservice.model.enums.TestStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO for A/B test response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AbTestResponse {

    /**
     * Test ID
     */
    private String id;

    /**
     * Test name
     */
    private String name;

    /**
     * Test description
     */
    private String description;

    /**
     * Test status
     */
    private TestStatus status;

    /**
     * User who created the test
     */
    private String createdBy;

    /**
     * Creation timestamp
     */
    private LocalDateTime createdAt;

    /**
     * Last update timestamp
     */
    private LocalDateTime updatedAt;

    /**
     * Test start timestamp
     */
    private LocalDateTime startedAt;

    /**
     * Test completion timestamp
     */
    private LocalDateTime completedAt;

    /**
     * Control version details
     */
    private VersionInfo controlVersion;

    /**
     * Variant version details
     */
    private VersionInfo variantVersion;

    /**
     * Target sample size
     */
    private Integer sampleSize;

    /**
     * Confidence threshold
     */
    private Double confidenceThreshold;

    /**
     * Evaluation metric
     */
    private String evaluationMetric;

    /**
     * Test parameters
     */
    private Map<String, Object> testParameters;

    /**
     * Success criteria
     */
    private String successCriteria;

    /**
     * Provider ID
     */
    private String providerId;

    /**
     * Model ID
     */
    private String modelId;

    /**
     * Current progress (percentage)
     */
    private Double progress;

    /**
     * Test results
     */
    private List<ResultInfo> results;

    /**
     * Summary of test outcome
     */
    private TestOutcome outcome;

    /**
     * Version information DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VersionInfo {
        private String id;
        private String versionNumber;
        private String templateId;
        private String templateName;
    }

    /**
     * Result information DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResultInfo {
        private String id;
        private String versionId;
        private Boolean isControlVersion;
        private Integer sampleCount;
        private Integer successCount;
        private Double successRate;
        private Double averageResponseTime;
        private Double averageTokens;
        private Double averageCost;
        private Double totalCost;
        private Double pValue;
        private Double confidenceLevel;
        private Map<String, Object> metricValues;
    }

    /**
     * Test outcome DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestOutcome {
        private String winnerId;
        private String winnerName;
        private Double improvementPercentage;
        private Boolean significantDifference;
        private Double confidenceLevel;
        private String recommendation;
    }
}