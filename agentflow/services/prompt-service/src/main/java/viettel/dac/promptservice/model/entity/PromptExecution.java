package viettel.dac.promptservice.model.entity;


import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import viettel.dac.promptservice.model.converter.JsonAttributeConverter;
import viettel.dac.promptservice.model.enums.ExecutionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Prompt execution entity
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
@Builder
public class PromptExecution extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "version_id", nullable = false)
    private PromptVersion version;

    @Column(name = "provider_id", nullable = false)
    private String providerId;

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

    @Column(name = "executed_at", nullable = false)
    private LocalDateTime executedAt;

    @CreatedBy
    @Column(name = "executed_by", nullable = false, updatable = false)
    private String executedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExecutionStatus status;
}