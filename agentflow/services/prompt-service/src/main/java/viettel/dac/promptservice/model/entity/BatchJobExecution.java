package viettel.dac.promptservice.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;
import viettel.dac.promptservice.model.converter.JsonAttributeConverter;
import viettel.dac.promptservice.model.enums.BatchJobStatus;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Entity for batch job executions
 */
@Entity
@Table(name = "batch_job_executions", indexes = {
        @Index(name = "idx_execution_job", columnList = "job_id"),
        @Index(name = "idx_execution_status", columnList = "status"),
        @Index(name = "idx_execution_started", columnList = "started_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class BatchJobExecution extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private BatchJob job;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BatchJobStatus status;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * Duration in milliseconds
     */
    @Column(name = "duration_ms")
    private Long durationMs;

    /**
     * Host/worker that processed this execution
     */
    @Column(name = "worker_id")
    private String workerId;

    /**
     * Parameters used for this execution
     */
    @Convert(converter = JsonAttributeConverter.class)
    @Column(name = "execution_parameters", columnDefinition = "TEXT")
    private Map<String, Object> executionParameters;

    /**
     * Result of the execution
     */
    @Convert(converter = JsonAttributeConverter.class)
    @Column(name = "execution_result", columnDefinition = "TEXT")
    private Map<String, Object> executionResult;

    /**
     * Detailed log of the execution
     */
    @Column(name = "execution_log", columnDefinition = "TEXT")
    private String executionLog;

    /**
     * Error message if execution failed
     */
    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    /**
     * Start the execution
     */
    public void start() {
        this.startedAt = LocalDateTime.now();
        this.status = BatchJobStatus.RUNNING;
    }

    /**
     * Complete the execution successfully
     */
    public void complete(Map<String, Object> result) {
        this.completedAt = LocalDateTime.now();
        this.status = BatchJobStatus.COMPLETED;
        this.executionResult = result;
        calculateDuration();
    }

    /**
     * Mark the execution as failed
     */
    public void fail(String errorMessage) {
        this.completedAt = LocalDateTime.now();
        this.status = BatchJobStatus.FAILED;
        this.errorMessage = errorMessage;
        calculateDuration();
    }

    /**
     * Calculate execution duration
     */
    private void calculateDuration() {
        if (startedAt != null && completedAt != null) {
            this.durationMs = Duration.between(startedAt, completedAt).toMillis();
        }
    }

    /**
     * Check if execution is running
     */
    public boolean isRunning() {
        return status == BatchJobStatus.RUNNING;
    }

    /**
     * Check if execution is completed
     */
    public boolean isCompleted() {
        return status == BatchJobStatus.COMPLETED ||
                status == BatchJobStatus.FAILED ||
                status == BatchJobStatus.CANCELLED;
    }

    /**
     * Check if execution is successful
     */
    public boolean isSuccessful() {
        return status == BatchJobStatus.COMPLETED;
    }

    /**
     * Append to execution log
     */
    public void appendToLog(String logEntry) {
        if (this.executionLog == null) {
            this.executionLog = "";
        }
        this.executionLog += logEntry + "\n";
    }
}