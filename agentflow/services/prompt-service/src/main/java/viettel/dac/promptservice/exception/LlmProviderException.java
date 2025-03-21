package viettel.dac.promptservice.exception;

import lombok.Getter;

/**
 * Exception thrown when an error occurs during LLM provider operations
 */
@Getter
public class LlmProviderException extends RuntimeException {

    public enum ErrorType {
        AUTHENTICATION,
        RATE_LIMIT,
        CONTEXT_LENGTH,
        CONTENT_FILTER,
        INVALID_REQUEST,
        SERVICE_UNAVAILABLE,
        TIMEOUT,
        UNKNOWN
    }

    private final String providerId;
    private final String modelId;
    private final ErrorType errorType;
    private final int statusCode;

    public LlmProviderException(String message, String providerId, String modelId, ErrorType errorType) {
        super(message);
        this.providerId = providerId;
        this.modelId = modelId;
        this.errorType = errorType;
        this.statusCode = mapErrorTypeToStatusCode(errorType);
    }

    public LlmProviderException(String message, Throwable cause, String providerId, String modelId, ErrorType errorType) {
        super(message, cause);
        this.providerId = providerId;
        this.modelId = modelId;
        this.errorType = errorType;
        this.statusCode = mapErrorTypeToStatusCode(errorType);
    }

    /**
     * Maps the error type to an appropriate HTTP status code
     */
    private int mapErrorTypeToStatusCode(ErrorType errorType) {
        return switch (errorType) {
            case AUTHENTICATION -> 401;
            case RATE_LIMIT -> 429;
            case CONTEXT_LENGTH, CONTENT_FILTER, INVALID_REQUEST -> 400;
            case SERVICE_UNAVAILABLE -> 503;
            case TIMEOUT -> 408;
            case UNKNOWN -> 500;
        };
    }
}