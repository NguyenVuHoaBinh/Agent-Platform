package viettel.dac.promptservice.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedBy;
import viettel.dac.promptservice.model.converter.JsonAttributeConverter;
import viettel.dac.promptservice.model.enums.ExecutionStatus;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Prompt execution entity with enhanced metrics and analytics capabilities
 */
@Entity
@Table(name = "prompt_executions", indexes = {
        @Index(name = "idx_execution_version", columnList = "version_id"),
        @Index(name = "idx_execution_provider", columnList = "provider_id"),
        @Index(name = "idx_execution_model", columnList = "model_id"),
        @Index(name = "idx_execution_status", columnList = "status"),
        @Index(name = "idx_execution_executed_at", columnList = "executed_at"),
        @Index(name = "idx_execution_executed_by", columnList = "executed_by")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class PromptExecution extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "version_id", nullable = false)
    private PromptVersion version;

    @NotBlank(message = "Provider ID is required")
    @Column(name = "provider_id", nullable = false)
    private String providerId;

    @NotBlank(message = "Model ID is required")
    @Column(name = "model_id", nullable = false)
    private String modelId;

    @Convert(converter = JsonAttributeConverter.class)
    @Column(name = "input_parameters", columnDefinition = "TEXT")
    private Map<String, Object> inputParameters;

    @Column(name = "raw_response", columnDefinition = "TEXT")
    private String rawResponse;

    @Column(name = "token_count")
    private Integer tokenCount;

    @Column(name = "input_tokens")
    private Integer inputTokens;

    @Column(name = "output_tokens")
    private Integer outputTokens;

    @Column(precision = 10, scale = 6)
    private BigDecimal cost;

    @Column(name = "response_time_ms")
    private Long responseTimeMs;

    @NotNull(message = "Execution timestamp is required")
    @PastOrPresent(message = "Execution timestamp cannot be in the future")
    @Column(name = "executed_at", nullable = false)
    private LocalDateTime executedAt;

    @CreatedBy
    @Column(name = "executed_by", nullable = false, updatable = false)
    private String executedBy;

    @NotNull(message = "Execution status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExecutionStatus status;

    /**
     * Calculate the total cost of this execution based on input and output tokens
     *
     * @param inputTokenCost Cost per input token
     * @param outputTokenCost Cost per output token
     * @return Calculated cost
     */
    public BigDecimal calculateCost(BigDecimal inputTokenCost, BigDecimal outputTokenCost) {
        if (inputTokens == null || outputTokens == null) {
            return null;
        }

        BigDecimal inputCost = inputTokenCost.multiply(BigDecimal.valueOf(inputTokens));
        BigDecimal outputCost = outputTokenCost.multiply(BigDecimal.valueOf(outputTokens));

        return inputCost.add(outputCost);
    }

    /**
     * Set metrics from execution start and end times
     *
     * @param startTime Execution start time
     * @param endTime Execution end time
     */
    public void setMetricsFromExecutionTime(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime != null && endTime != null) {
            this.executedAt = startTime;
            this.responseTimeMs = Duration.between(startTime, endTime).toMillis();
        }
    }

    /**
     * Check if execution was successful
     *
     * @return true if status is SUCCESS
     */
    public boolean isSuccessful() {
        return status == ExecutionStatus.SUCCESS;
    }

    /**
     * Check if execution timed out
     *
     * @return true if status is TIMEOUT
     */
    public boolean isTimeout() {
        return status == ExecutionStatus.TIMEOUT;
    }

    /**
     * Check if execution had validation errors
     *
     * @return true if status is INVALID_PARAMS
     */
    public boolean hasValidationErrors() {
        return status == ExecutionStatus.INVALID_PARAMS;
    }

    /**
     * Calculate tokens per second rate
     *
     * @return tokens per second or null if data is missing
     */
    public Double getTokensPerSecond() {
        if (tokenCount == null || responseTimeMs == null || responseTimeMs == 0) {
            return null;
        }

        return (tokenCount * 1000.0) / responseTimeMs;
    }
}