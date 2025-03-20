package viettel.dac.promptservice.model.enums;

/**
 * Status of a prompt execution
 */
public enum ExecutionStatus {
    SUCCESS("Successful execution"),
    ERROR("Failed with error"),
    TIMEOUT("Execution timed out"),
    INVALID_PARAMS("Failed due to invalid parameters"),
    PROVIDER_ERROR("Provider service returned an error"),
    RATE_LIMITED("Request was rate limited");

    private final String description;

    ExecutionStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}