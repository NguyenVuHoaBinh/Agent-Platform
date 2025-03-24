package viettel.dac.promptservice.model.enums;

/**
 * Enum for batch job status
 */
public enum BatchJobStatus {
    /**
     * Job is created but not yet scheduled or running
     */
    PENDING,

    /**
     * Job is scheduled to run at a specific time
     */
    SCHEDULED,

    /**
     * Job is currently running
     */
    RUNNING,

    /**
     * Job completed successfully
     */
    COMPLETED,

    /**
     * Job failed
     */
    FAILED,

    /**
     * Job was cancelled
     */
    CANCELLED
}