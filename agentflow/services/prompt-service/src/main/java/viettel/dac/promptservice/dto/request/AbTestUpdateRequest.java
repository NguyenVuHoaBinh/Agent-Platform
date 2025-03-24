package viettel.dac.promptservice.dto.request;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import viettel.dac.promptservice.model.enums.TestStatus;

import java.util.Map;

/**
 * DTO for updating an A/B test
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AbTestUpdateRequest {

    /**
     * Name of the test
     */
    private String name;

    /**
     * Description of the test
     */
    private String description;

    /**
     * Target sample size for each variant
     */
    @Min(value = 1, message = "Sample size must be at least 1")
    private Integer sampleSize;

    /**
     * Confidence threshold (0-100%)
     */
    @Min(value = 0, message = "Confidence threshold must be non-negative")
    private Double confidenceThreshold;

    /**
     * Evaluation metric
     */
    private String evaluationMetric;

    /**
     * Test parameters (for parameter substitution in prompts)
     */
    private Map<String, Object> testParameters;

    /**
     * Custom success criteria
     */
    private String successCriteria;

    /**
     * Provider ID for test execution
     */
    private String providerId;

    /**
     * Model ID for test execution
     */
    private String modelId;

    /**
     * New status for the test
     */
    private TestStatus status;
}