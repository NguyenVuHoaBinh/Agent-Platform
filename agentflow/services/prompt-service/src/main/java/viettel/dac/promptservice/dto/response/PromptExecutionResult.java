package viettel.dac.promptservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import viettel.dac.promptservice.dto.validation.ValidationResult;
import viettel.dac.promptservice.model.enums.ExecutionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO for the result of a prompt execution test
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptExecutionResult {

    /**
     * Execution ID in the database (if stored)
     */
    private String executionId;

    /**
     * Prompt version ID that was tested
     */
    private String versionId;

    /**
     * Template ID associated with the version
     */
    private String templateId;

    /**
     * Provider ID used for testing
     */
    private String providerId;

    /**
     * Model ID used for testing
     */
    private String modelId;

    /**
     * Parameters used in the test
     */
    private Map<String, Object> parameters;

    /**
     * The text response from the LLM
     */
    private String response;

    /**
     * Total token count (input + output)
     */
    private Integer tokenCount;

    /**
     * Input token count
     */
    private Integer inputTokens;

    /**
     * Output token count
     */
    private Integer outputTokens;

    /**
     * Estimated cost of the execution
     */
    private BigDecimal cost;

    /**
     * Response time in milliseconds
     */
    private Long responseTimeMs;

    /**
     * When the test was executed
     */
    private LocalDateTime executedAt;

    /**
     * Who executed the test
     */
    private String executedBy;

    /**
     * Execution status
     */
    private ExecutionStatus status;

    /**
     * Error message (if status is not SUCCESS)
     */
    private String errorMessage;

    /**
     * Validation result (if validation was requested)
     */
    private ValidationResult validationResult;

    /**
     * Whether the validation passed
     */
    private Boolean validationPassed;

    /**
     * Provider-specific metadata about the execution
     */
    private Map<String, Object> metadata;

    /**
     * Tokens per second processing rate
     */
    public Double getTokensPerSecond() {
        if (tokenCount == null || responseTimeMs == null || responseTimeMs == 0) {
            return null;
        }
        return (tokenCount * 1000.0) / responseTimeMs;
    }
}