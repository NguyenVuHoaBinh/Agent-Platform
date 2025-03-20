package viettel.dac.promptservice.exception;

/**
 * Exception thrown when a requested resource is not found
 */
public class ResourceNotFoundException extends PromptServiceException {
    private static final String ERROR_CODE = "RESOURCE_NOT_FOUND";

    public ResourceNotFoundException(String message) {
        super(message, ERROR_CODE);
    }

    public ResourceNotFoundException(String resourceType, String identifier) {
        super(resourceType + " not found with identifier: " + identifier, ERROR_CODE);
    }
}