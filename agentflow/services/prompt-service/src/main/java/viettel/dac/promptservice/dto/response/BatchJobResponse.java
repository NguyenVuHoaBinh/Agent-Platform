package viettel.dac.promptservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import viettel.dac.promptservice.model.enums.BatchJobStatus;
import viettel.dac.promptservice.model.enums.BatchJobType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO for batch job response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchJobResponse {

    /**
     * Job ID
     */
    private String id;

    /**
     * Job name
     */
    private String name;

    /**
     * Job description
     */
    private String description;

    /**
     * Job type
     */
    private BatchJobType jobType;

    /**
     * Job status
     */
    private BatchJobStatus status;

    /**
     * Creator ID
     */
    private String createdBy;

    /**
     * Creation timestamp
     */
    private LocalDateTime createdAt;

    /**
     * Update timestamp
     */
    private LocalDateTime updatedAt;

    /**
     * Scheduled time
     */
    private LocalDateTime scheduledAt;

    /**
     * Start time
     */
    private LocalDateTime startedAt;

    /**
     * Completion time
     */
    private LocalDateTime completedAt;

    /**
     * Job priority
     */
    private Integer priority;

    /**
     * Maximum number of retries
     */
    private Integer maxRetries;

    /**
     * Current retry count
     */
    private Integer retryCount;

    /**
     * Error message if job failed
     */
    private String errorMessage;

    /**
     * Completion percentage
     */
    private Integer completionPercentage;

    /**
     * Template details
     */
    private TemplateInfo template;

    /**
     * Version details
     */
    private VersionInfo version;

    /**
     * Job parameters
     */
    private Map<String, Object> parameters;

    /**
     * Job configuration
     */
    private Map<String, Object> configuration;

    /**
     * Job result
     */
    private Map<String, Object> result;

    /**
     * Job executions
     */
    private List<ExecutionInfo> executions;

    /**
     * Time remaining estimation (in seconds)
     */
    private Long estimatedTimeRemaining;

    /**
     * Latest execution log (if exists)
     */
    private String latestExecutionLog;

    /**
     * Template information DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemplateInfo {
        private String id;
        private String name;
    }

    /**
     * Version information DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VersionInfo {
        private String id;
        private String versionNumber;
    }

    /**
     * Execution information DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExecutionInfo {
        private String id;
        private BatchJobStatus status;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;
        private Long durationMs;
        private String workerId;
        private String errorMessage;
    }
}