package viettel.dac.promptservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import viettel.dac.promptservice.dto.request.LlmRequest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DTO representing a response from an LLM provider
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmResponse {

    /**
     * Original request information
     */
    private LlmRequest request;

    /**
     * Primary generated text response
     */
    private String text;

    /**
     * Alternative responses if multiple were requested
     */
    @Builder.Default
    private List<String> alternatives = new ArrayList<>();

    /**
     * Number of input tokens in the request
     */
    private Integer inputTokenCount;

    /**
     * Number of output tokens in the response
     */
    private Integer outputTokenCount;

    /**
     * Total number of tokens (input + output)
     */
    private Integer totalTokenCount;

    /**
     * Response time in milliseconds
     */
    private Long responseTimeMs;

    /**
     * Execution start timestamp
     */
    private LocalDateTime startTime;

    /**
     * Execution completion timestamp
     */
    private LocalDateTime completionTime;

    /**
     * Estimated cost of the request in USD
     */
    private Double cost;

    /**
     * Raw response from the provider
     */
    private String rawResponse;

    /**
     * Boolean indicating if the request was successful
     */
    private boolean successful;

    /**
     * Error message if the request failed
     */
    private String errorMessage;

    /**
     * Provider-specific metadata
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    /**
     * Calculate duration based on start and completion time
     * @return Duration in milliseconds
     */
    public Long calculateDuration() {
        if (startTime != null && completionTime != null) {
            return java.time.Duration.between(startTime, completionTime).toMillis();
        }
        return responseTimeMs;
    }
}