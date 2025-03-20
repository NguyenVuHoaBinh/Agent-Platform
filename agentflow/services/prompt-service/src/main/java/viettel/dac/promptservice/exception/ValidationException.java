package viettel.dac.promptservice.exception;

import java.util.Map;

/**
 * Exception thrown when input data fails validation
 */
public class ValidationException extends PromptServiceException {
    private static final String ERROR_CODE = "VALIDATION_ERROR";

    private final Map<String, String> errors;

    public ValidationException(String message) {
        super(message, ERROR_CODE);
        this.errors = null;
    }

    public ValidationException(String message, Map<String, String> errors) {
        super(message, ERROR_CODE);
        this.errors = errors;
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}