package viettel.dac.promptservice.dto.request;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import viettel.dac.promptservice.model.enums.BatchJobStatus;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO for updating a batch job
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchJobUpdateRequest {

    /**
     * Name of the job
     */
    private String name;

    /**
     * Description of the job
     */
    private String description;

    /**
     * Status of the job
     */
    private BatchJobStatus status;

    /**
     * Time to schedule the job
     */
    private LocalDateTime scheduledAt;

    /**
     * Priority of the job (1-10, higher number = higher priority)
     */
    @Min(value = 1, message = "Priority must be at least 1")
    private Integer priority;

    /**
     * Number of retries in case of failure
     */
    @Min(value = 0, message = "Max retries must be non-negative")
    private Integer maxRetries;

    /**
     * Parameters for the job
     */
    private Map<String, Object> parameters;

    /**
     * Configuration for the job
     */
    private Map<String, Object> configuration;
}