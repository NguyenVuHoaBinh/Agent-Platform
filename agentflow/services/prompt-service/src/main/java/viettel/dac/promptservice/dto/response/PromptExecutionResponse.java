package viettel.dac.promptservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import viettel.dac.promptservice.model.enums.ExecutionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO for returning prompt execution results
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PromptExecutionResponse {

    private String id;
    private String versionId;
    private String templateId;
    private String providerId;
    private String modelId;
    private Map<String, Object> inputParameters;
    private String response;
    private Integer tokenCount;
    private Integer inputTokens;
    private Integer outputTokens;
    private BigDecimal cost;
    private Long responseTimeMs;
    private LocalDateTime executedAt;
    private String executedBy;
    private ExecutionStatus status;
}