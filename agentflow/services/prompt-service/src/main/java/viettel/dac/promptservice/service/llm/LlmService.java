package viettel.dac.promptservice.service.llm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import viettel.dac.promptservice.dto.request.LlmRequest;
import viettel.dac.promptservice.dto.response.LlmResponse;
import viettel.dac.promptservice.exception.LlmProviderException;
import viettel.dac.promptservice.model.entity.PromptExecution;
import viettel.dac.promptservice.model.entity.PromptVersion;
import viettel.dac.promptservice.model.enums.ExecutionStatus;
import viettel.dac.promptservice.repository.jpa.PromptExecutionRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Service for executing prompts against LLM providers
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LlmService {

    private final LlmProviderFactory providerFactory;
    private final LlmProviderProperties providerProperties;
    private final PromptExecutionRepository executionRepository;

    /**
     * Execute a prompt against an LLM provider and record the execution
     *
     * @param promptVersion The prompt version to execute
     * @param providerId The provider ID
     * @param modelId The model ID
     * @param parameters The parameters to apply to the prompt
     * @return PromptExecution with the execution results
     */
    public PromptExecution executePrompt(PromptVersion promptVersion, String providerId,
                                         String modelId, Map<String, Object> parameters) {
        log.debug("Executing prompt version {} with provider {}, model {}",
                promptVersion.getId(), providerId, modelId);

        LocalDateTime startTime = LocalDateTime.now();
        PromptExecution execution = PromptExecution.builder()
                .version(promptVersion)
                .providerId(providerId)
                .modelId(modelId)
                .inputParameters(parameters)
                .executedAt(startTime)
                .status(ExecutionStatus.SUCCESS) // Will be updated if there's an error
                .build();

        try {
            // Apply parameters to the prompt template
            String promptText = promptVersion.applyParameters(parameters);

            // Get the provider
            LlmProvider provider = providerFactory.getProvider(providerId)
                    .orElseThrow(() -> new LlmProviderException(
                            "Provider not found: " + providerId,
                            providerId, modelId, LlmProviderException.ErrorType.INVALID_REQUEST));

            // Build the request with system prompt from version
            LlmRequest request = LlmRequest.builder()
                    .providerId(providerId)
                    .modelId(modelId)
                    .prompt(promptText)
                    .systemPrompt(promptVersion.getSystemPrompt())
                    .maxTokens(providerProperties.getDefaultMaxTokens())
                    .temperature(providerProperties.getDefaultTemperature())
                    .timeoutMs(providerProperties.getDefaultTimeoutMs())
                    .build();

            // Execute the prompt
            LlmResponse response = provider.executePrompt(request);

            // Update the execution with the results
            execution.setRawResponse(response.getText());
            execution.setInputTokens(response.getInputTokenCount());
            execution.setOutputTokens(response.getOutputTokenCount());
            execution.setTokenCount(response.getTotalTokenCount());
            execution.setResponseTimeMs(response.getResponseTimeMs());
            execution.setCost(BigDecimal.valueOf(response.getCost()));
            execution.setStatus(ExecutionStatus.SUCCESS);

        } catch (LlmProviderException e) {
            log.error("Error executing prompt: {}", e.getMessage());
            execution.setRawResponse("Error: " + e.getMessage());
            execution.setStatus(mapToExecutionStatus(e.getErrorType()));
        }

        // Save and return the execution record
        return executionRepository.save(execution);
    }

    /**
     * Execute a prompt asynchronously
     *
     * @param promptVersion The prompt version to execute
     * @param providerId The provider ID
     * @param modelId The model ID
     * @param parameters The parameters to apply to the prompt
     * @return CompletableFuture with the execution results
     */
    public CompletableFuture<PromptExecution> executePromptAsync(PromptVersion promptVersion,
                                                                 String providerId,
                                                                 String modelId,
                                                                 Map<String, Object> parameters) {
        return CompletableFuture.supplyAsync(() ->
                executePrompt(promptVersion, providerId, modelId, parameters));
    }

    /**
     * Get all available providers
     *
     * @return Map of provider IDs to provider names
     */
    public Map<String, List<String>> getAvailableProviders() {
        return providerFactory.getAllProviders().values().stream()
                .collect(Collectors.toMap(
                        LlmProvider::getProviderId,
                        provider -> provider.getAvailableModels().keySet().stream().toList()
                ));
    }

    /**
     * Get available models for a provider
     *
     * @param providerId The provider ID
     * @return Map of model IDs to model names
     */
    public Map<String, String> getAvailableModels(String providerId) {
        return providerFactory.getProvider(providerId)
                .map(LlmProvider::getAvailableModels)
                .orElse(Map.of());
    }

    /**
     * Check if a provider is available
     *
     * @param providerId The provider ID
     * @return true if the provider is available
     */
    public boolean isProviderAvailable(String providerId) {
        return providerFactory.hasProvider(providerId);
    }

    /**
     * Map LlmProviderException error type to ExecutionStatus
     */
    private ExecutionStatus mapToExecutionStatus(LlmProviderException.ErrorType errorType) {
        return switch (errorType) {
            case AUTHENTICATION -> ExecutionStatus.PROVIDER_ERROR;
            case RATE_LIMIT -> ExecutionStatus.RATE_LIMITED;
            case CONTEXT_LENGTH -> ExecutionStatus.INVALID_PARAMS;
            case CONTENT_FILTER -> ExecutionStatus.PROVIDER_ERROR;
            case INVALID_REQUEST -> ExecutionStatus.INVALID_PARAMS;
            case SERVICE_UNAVAILABLE -> ExecutionStatus.PROVIDER_ERROR;
            case TIMEOUT -> ExecutionStatus.TIMEOUT;
            case UNKNOWN -> ExecutionStatus.ERROR;
        };
    }
}