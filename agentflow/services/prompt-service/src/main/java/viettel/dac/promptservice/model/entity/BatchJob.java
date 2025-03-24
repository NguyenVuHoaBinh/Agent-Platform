package viettel.dac.promptservice.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedBy;
import viettel.dac.promptservice.model.converter.JsonAttributeConverter;
import viettel.dac.promptservice.model.enums.BatchJobStatus;
import viettel.dac.promptservice.model.enums.BatchJobType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Entity for batch job definitions
 */
@Entity
@Table(name = "batch_jobs", indexes = {
        @Index(name = "idx_job_status", columnList = "status"),
        @Index(name = "idx_job_type", columnList = "job_type"),
        @Index(name = "idx_job_created_by", columnList = "created_by"),
        @Index(name = "idx_job_scheduled_at", columnList = "scheduled_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class BatchJob extends BaseEntity {

    @NotBlank(message = "Job name is required")
    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @NotNull(message = "Job type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false)
    private BatchJobType jobType;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BatchJobStatus status;

    @CreatedBy
    @Column(name = "created_by", nullable = false, updatable = false)
    private String createdBy;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * Priority of the job (1-10, higher number = higher priority)
     */
    @Column(nullable = false)
    private Integer priority;

    /**
     * Number of retries in case of failure
     */
    @Column(name = "max_retries", nullable = false)
    private Integer maxRetries;

    /**
     * Number of retry attempts so far
     */
    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;

    /**
     * Error message if job failed
     */
    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    /**
     * Percentage of completion (0-100)
     */
    @Column(name = "completion_percentage")
    private Integer completionPercentage;

    /**
     * Reference to the prompt template for this job
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private PromptTemplate template;

    /**
     * Reference to the prompt version for this job
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "version_id")
    private PromptVersion version;

    /**
     * Parameters for the job
     */
    @Convert(converter = JsonAttributeConverter.class)
    @Column(name = "parameters", columnDefinition = "TEXT")
    private Map<String, Object> parameters;

    /**
     * Configuration for the job
     */
    @Convert(converter = JsonAttributeConverter.class)
    @Column(name = "configuration", columnDefinition = "TEXT")
    private Map<String, Object> configuration;

    /**
     * Result of the job
     */
    @Convert(converter = JsonAttributeConverter.class)
    @Column(name = "result", columnDefinition = "TEXT")
    private Map<String, Object> result;

    /**
     * Job executions
     */
    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BatchJobExecution> executions = new ArrayList<>();

    /**
     * Add an execution to this job
     */
    public void addExecution(BatchJobExecution execution) {
        executions.add(execution);
        execution.setJob(this);
    }

    /**
     * Remove an execution from this job
     */
    public void removeExecution(BatchJobExecution execution) {
        executions.remove(execution);
        execution.setJob(null);
    }

    /**
     * Check if job is pending
     */
    public boolean isPending() {
        return status == BatchJobStatus.PENDING || status == BatchJobStatus.SCHEDULED;
    }

    /**
     * Check if job is running
     */
    public boolean isRunning() {
        return status == BatchJobStatus.RUNNING;
    }

    /**
     * Check if job is completed
     */
    public boolean isCompleted() {
        return status == BatchJobStatus.COMPLETED || status == BatchJobStatus.FAILED ||
                status == BatchJobStatus.CANCELLED;
    }

    /**
     * Check if job is successful
     */
    public boolean isSuccessful() {
        return status == BatchJobStatus.COMPLETED;
    }

    /**
     * Check if job is failed
     */
    public boolean isFailed() {
        return status == BatchJobStatus.FAILED;
    }

    /**
     * Check if job should be retried
     */
    public boolean shouldRetry() {
        return isFailed() && retryCount < maxRetries;
    }

    /**
     * Get latest execution
     */
    public BatchJobExecution getLatestExecution() {
        if (executions == null || executions.isEmpty()) {
            return null;
        }

        return executions.stream()
                .max((e1, e2) -> e1.getStartedAt().compareTo(e2.getStartedAt()))
                .orElse(null);
    }

    /**
     * Update progress
     */
    public void updateProgress(int progress) {
        this.completionPercentage = progress;
        if (progress >= 100) {
            this.status = BatchJobStatus.COMPLETED;
            this.completedAt = LocalDateTime.now();
        }
    }
}