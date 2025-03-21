package viettel.dac.promptservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * DTO for a request to test a prompt version against an LLM provider
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptTestRequest {

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
     * Parameter values to substitute in the prompt
     */
    @Builder.Default
    private Map<String, Object> parameters = new HashMap<>();

    /**
     * Maximum tokens to generate in the response
     */
    private Integer maxTokens;

    /**
     * Temperature setting for the model (controls randomness)
     */
    private Double temperature;

    /**
     * Validation criteria to apply to the response
     */
    @Builder.Default
    private Map<String, Object> validationCriteria = new HashMap<>();

    /**
     * Whether to store the execution in the database
     */
    @Builder.Default
    private boolean storeResult = true;
}
