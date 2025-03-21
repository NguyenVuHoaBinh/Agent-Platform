package viettel.dac.promptservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DTO for a request to batch test a prompt version with multiple parameter sets
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptBatchTestRequest {

    /**
     * Prompt version ID to test
     */
    @NotBlank(message = "Version ID is required")
    private String versionId;

    /**
     * LLM provider ID to use for testing
     */
    @NotBlank(message = "Provider ID is required")
    private String providerId;

    /**
     * Model ID from the provider to use
     */
    @NotBlank(message = "Model ID is required")
    private String modelId;

    /**
     * List of parameter sets to test
     */
    @NotEmpty(message = "At least one parameter set is required")
    @Builder.Default
    private List<Map<String, Object>> parameterSets = new ArrayList<>();

    /**
     * Maximum tokens to generate in the response
     */
    private Integer maxTokens;

    /**
     * Temperature setting for the model (controls randomness)
     */
    private Double temperature;

    /**
     * Validation criteria to apply to all responses
     */
    @Builder.Default
    private Map<String, Object> validationCriteria = new HashMap<>();

    /**
     * Whether to execute tests in parallel
     */
    @Builder.Default
    private boolean parallelExecution = true;

    /**
     * Maximum concurrent executions if running in parallel
     */
    @Builder.Default
    private int maxConcurrent = 5;

    /**
     * Whether to store the executions in the database
     */
    @Builder.Default
    private boolean storeResults = true;
}
