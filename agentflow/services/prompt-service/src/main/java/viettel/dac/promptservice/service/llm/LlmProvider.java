package viettel.dac.promptservice.service.llm;


import viettel.dac.promptservice.dto.request.LlmRequest;
import viettel.dac.promptservice.dto.response.LlmResponse;
import viettel.dac.promptservice.exception.LlmProviderException;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Core interface for all LLM providers
 */
public interface LlmProvider {

    /**
     * Get the provider identifier
     * @return Unique provider ID
     */
    String getProviderId();

    /**
     * Get available models from this provider
     * @return Map of model IDs to human-readable names
     */
    Map<String, String> getAvailableModels();

    /**
     * Check if a specific model is supported by this provider
     * @param modelId The model ID to check
     * @return true if the model is supported
     */
    boolean supportsModel(String modelId);

    /**
     * Execute a prompt against the specified model
     * @param request The LLM request containing prompt and parameters
     * @return Response from the LLM with generated text and metadata
     * @throws LlmProviderException if an error occurs during execution
     */
    LlmResponse executePrompt(LlmRequest request) throws LlmProviderException;

    /**
     * Execute a prompt asynchronously against the specified model
     * @param request The LLM request containing prompt and parameters
     * @return CompletableFuture that will resolve to the LLM response
     */
    CompletableFuture<LlmResponse> executePromptAsync(LlmRequest request);

    /**
     * Count tokens in a prompt for a specific model
     * @param prompt The prompt text
     * @param modelId The model ID
     * @return Token count
     */
    int countTokens(String prompt, String modelId);

    /**
     * Calculate the estimated cost for a request based on token count
     * @param inputTokens Input token count
     * @param outputTokens Output token count (can be estimated)
     * @param modelId The model ID
     * @return Estimated cost in USD
     */
    double calculateCost(int inputTokens, int outputTokens, String modelId);

    /**
     * Get the maximum context length for a specific model
     * @param modelId The model ID
     * @return Maximum context length in tokens
     */
    int getMaxContextLength(String modelId);
}