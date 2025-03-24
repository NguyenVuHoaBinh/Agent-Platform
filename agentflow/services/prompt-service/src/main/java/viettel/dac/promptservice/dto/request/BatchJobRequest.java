package viettel.dac.promptservice.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import viettel.dac.promptservice.model.enums.BatchJobType;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * DTO for creating a batch job
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchJobRequest {

    /**
     * Name of the job
     */
    @NotBlank(message = "Job name is required")
    private String name;

    /**
     * Description of the job
     */
    private String description;

    /**
     * Type of job
     */
    @NotNull(message = "Job type is required")
    private BatchJobType jobType;

    /**
     * Template ID for the job
     */
    private String templateId;

    /**
     * Version ID for the job
     */
    private String versionId;

    /**
     * Time to schedule the job
     */
    private LocalDateTime scheduledAt;

    /**
     * Priority of the job (1-10, higher number = higher priority)
     */
    @Builder.Default
    @Min(value = 1, message = "Priority must be at least 1")
    private Integer priority = 5;

    /**
     * Number of retries in case of failure
     */
    @Builder.Default
    @Min(value = 0, message = "Max retries must be non-negative")
    private Integer maxRetries = 3;

    /**
     * Parameters for the job
     */
    @Builder.Default
    private Map<String, Object> parameters = new HashMap<>();

    /**
     * Configuration for the job
     */
    @Builder.Default
    private Map<String, Object> configuration = new HashMap<>();

    /**
     * Whether to start the job immediately
     */
    @Builder.Default
    private boolean startImmediately = false;
}