package viettel.dac.promptservice.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * DTO for creating an A/B test
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AbTestRequest {

    /**
     * Name of the test
     */
    @NotBlank(message = "Test name is required")
    private String name;

    /**
     * Description of the test
     */
    private String description;

    /**
     * Control version ID
     */
    @NotBlank(message = "Control version ID is required")
    private String controlVersionId;

    /**
     * Variant version ID
     */
    @NotBlank(message = "Variant version ID is required")
    private String variantVersionId;

    /**
     * Target sample size for each variant
     */
    @NotNull(message = "Sample size is required")
    @Min(value = 1, message = "Sample size must be at least 1")
    private Integer sampleSize;

    /**
     * Confidence threshold (0-100%)
     */
    @NotNull(message = "Confidence threshold is required")
    @Min(value = 0, message = "Confidence threshold must be non-negative")
    private Double confidenceThreshold;

    /**
     * Evaluation metric
     */
    @NotBlank(message = "Evaluation metric is required")
    private String evaluationMetric;

    /**
     * Test parameters (for parameter substitution in prompts)
     */
    @Builder.Default
    private Map<String, Object> testParameters = new HashMap<>();

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
     * Whether to start the test immediately
     */
    @Builder.Default
    private boolean startImmediately = false;
}