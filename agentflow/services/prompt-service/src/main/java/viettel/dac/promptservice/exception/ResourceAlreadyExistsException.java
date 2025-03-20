package viettel.dac.promptservice.exception;

/**
 * Exception thrown when attempting to create a resource that already exists
 */
public class ResourceAlreadyExistsException extends PromptServiceException {
    private static final String ERROR_CODE = "RESOURCE_ALREADY_EXISTS";

    public ResourceAlreadyExistsException(String message) {
        super(message, ERROR_CODE);
    }

    public ResourceAlreadyExistsException(String resourceType, String identifier) {
        super(resourceType + " already exists with identifier: " + identifier, ERROR_CODE);
    }
}