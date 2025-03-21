package viettel.dac.promptservice.service.llm;

import lombok.extern.slf4j.Slf4j;
import viettel.dac.promptservice.dto.request.LlmRequest;
import viettel.dac.promptservice.dto.response.LlmResponse;
import viettel.dac.promptservice.exception.LlmProviderException;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * Base implementation with common functionality for all LLM providers
 */
@Slf4j
public abstract class BaseLlmProvider implements LlmProvider {

    protected final Executor executor;

    protected BaseLlmProvider(Executor executor) {
        this.executor = executor;
    }

    @Override
    public boolean supportsModel(String modelId) {
        return getAvailableModels().containsKey(modelId);
    }

    @Override
    public CompletableFuture<LlmResponse> executePromptAsync(LlmRequest request) {
        return CompletableFuture.supplyAsync(() -> {
                    try {
                        return executePrompt(request);
                    } catch (Exception e) {
                        log.error("Error executing prompt asynchronously with provider {} and model {}: {}",
                                request.getProviderId(), request.getModelId(), e.getMessage(), e);
                        throw new LlmProviderException("Async execution failed: " + e.getMessage(),
                                e, getProviderId(), request.getModelId(), LlmProviderException.ErrorType.UNKNOWN);
                    }
                }, executor).orTimeout(request.getTimeoutMs(), TimeUnit.MILLISECONDS)
                .exceptionally(e -> {
                    Throwable cause = e.getCause();
                    if (cause instanceof LlmProviderException) {
                        throw (LlmProviderException) cause;
                    }

                    LlmProviderException.ErrorType errorType = LlmProviderException.ErrorType.UNKNOWN;
                    if (e instanceof java.util.concurrent.TimeoutException) {
                        errorType = LlmProviderException.ErrorType.TIMEOUT;
                    }

                    throw new LlmProviderException("Execution failed: " + e.getMessage(),
                            e, getProviderId(), request.getModelId(), errorType);
                });
    }

    /**
     * Build a base response with timing information
     * @param request Original request
     * @param startTime Execution start time
     * @return Partially built response
     */
    protected LlmResponse.LlmResponseBuilder createBaseResponse(LlmRequest request, LocalDateTime startTime) {
        LocalDateTime completionTime = LocalDateTime.now();
        long responseTimeMs = java.time.Duration.between(startTime, completionTime).toMillis();

        return LlmResponse.builder()
                .request(request)
                .startTime(startTime)
                .completionTime(completionTime)
                .responseTimeMs(responseTimeMs);
    }

    /**
     * Validate that a request contains valid parameters for this provider
     * @param request The request to validate
     * @throws LlmProviderException if validation fails
     */
    protected void validateRequest(LlmRequest request) throws LlmProviderException {
        if (request == null) {
            throw new LlmProviderException("Request cannot be null",
                    getProviderId(), null, LlmProviderException.ErrorType.INVALID_REQUEST);
        }

        if (request.getPrompt() == null || request.getPrompt().trim().isEmpty()) {
            throw new LlmProviderException("Prompt cannot be empty",
                    getProviderId(), request.getModelId(), LlmProviderException.ErrorType.INVALID_REQUEST);
        }

        if (request.getModelId() == null || !supportsModel(request.getModelId())) {
            throw new LlmProviderException("Unsupported model: " + request.getModelId(),
                    getProviderId(), request.getModelId(), LlmProviderException.ErrorType.INVALID_REQUEST);
        }

        int tokenCount = countTokens(request.getPrompt(), request.getModelId());
        int maxContextLength = getMaxContextLength(request.getModelId());

        if (tokenCount > maxContextLength) {
            throw new LlmProviderException(
                    String.format("Prompt exceeds maximum context length. Token count: %d, Maximum: %d",
                            tokenCount, maxContextLength),
                    getProviderId(), request.getModelId(), LlmProviderException.ErrorType.CONTEXT_LENGTH);
        }
    }
}