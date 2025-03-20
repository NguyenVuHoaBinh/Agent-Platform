package viettel.dac.promptservice.exception;

import lombok.Getter;

/**
 * Base exception class for the Prompt Service application
 */
@Getter
public class PromptServiceException extends RuntimeException {
    private final String errorCode;

    public PromptServiceException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public PromptServiceException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}