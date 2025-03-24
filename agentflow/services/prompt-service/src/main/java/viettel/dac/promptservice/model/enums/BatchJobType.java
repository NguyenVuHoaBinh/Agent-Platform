package viettel.dac.promptservice.model.enums;

/**
 * Enum for batch job types
 */
public enum BatchJobType {
    /**
     * Job that optimizes a prompt version
     */
    PROMPT_OPTIMIZATION,

    /**
     * Job that runs a prompt version against a large dataset
     */
    BATCH_EXECUTION,

    /**
     * Job that generates variations of a prompt
     */
    PROMPT_VARIATION,

    /**
     * Job that imports data
     */
    DATA_IMPORT,

    /**
     * Job that exports data
     */
    DATA_EXPORT,

    /**
     * Job that analyzes prompt performance
     */
    PERFORMANCE_ANALYSIS,

    /**
     * Custom job type
     */
    CUSTOM
}