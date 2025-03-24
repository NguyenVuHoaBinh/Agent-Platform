package viettel.dac.promptservice.model.enums;

/**
 * Enum representing the status of an A/B test
 */
public enum TestStatus {
    /**
     * Test has been created but not yet started
     */
    CREATED,

    /**
     * Test is currently running
     */
    RUNNING,

    /**
     * Test has been paused
     */
    PAUSED,

    /**
     * Test has been completed
     */
    COMPLETED,

    /**
     * Test has been cancelled
     */
    CANCELLED
}