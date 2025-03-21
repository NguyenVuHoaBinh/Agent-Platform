package viettel.dac.promptservice.service.llm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import viettel.dac.promptservice.service.llm.providers.AnthropicProvider;
import viettel.dac.promptservice.service.llm.providers.OpenAiProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;

/**
 * Factory for creating and managing LLM provider instances
 */
@Component
@Slf4j
public class LlmProviderFactory {

    private final Map<String, LlmProvider> providers;
    private final WebClient.Builder webClientBuilder;
    private final Executor asyncExecutor;

    public LlmProviderFactory(WebClient.Builder webClientBuilder, @Qualifier("llmExecutor") Executor asyncExecutor,
                              LlmProviderProperties providerProperties) {
        this.webClientBuilder = webClientBuilder;
        this.asyncExecutor = asyncExecutor;
        this.providers = new HashMap<>();

        // Initialize configured providers
        initializeProviders(providerProperties);
    }

    /**
     * Get a provider by ID
     *
     * @param providerId The provider ID
     * @return Optional containing the provider if found
     */
    public Optional<LlmProvider> getProvider(String providerId) {
        return Optional.ofNullable(providers.get(providerId));
    }

    /**
     * Get all available providers
     *
     * @return Map of provider IDs to provider instances
     */
    public Map<String, LlmProvider> getAllProviders() {
        return new HashMap<>(providers);
    }

    /**
     * Check if a provider is available
     *
     * @param providerId The provider ID
     * @return true if the provider is available
     */
    public boolean hasProvider(String providerId) {
        return providers.containsKey(providerId);
    }

    /**
     * Get a provider that supports a specific model
     *
     * @param modelId The model ID
     * @return Optional containing the provider if found
     */
    public Optional<LlmProvider> getProviderForModel(String modelId) {
        return providers.values().stream()
                .filter(provider -> provider.supportsModel(modelId))
                .findFirst();
    }

    /**
     * Initialize providers based on configuration
     */
    private void initializeProviders(LlmProviderProperties properties) {
        // Initialize OpenAI provider if configured
        if (properties.getOpenai().isEnabled() && properties.getOpenai().getApiKey() != null) {
            try {
                OpenAiProvider openAiProvider = new OpenAiProvider(
                        webClientBuilder,
                        properties.getOpenai().getApiKey(),
                        asyncExecutor
                );
                providers.put(openAiProvider.getProviderId(), openAiProvider);
                log.info("Initialized OpenAI provider with {} models",
                        openAiProvider.getAvailableModels().size());
            } catch (Exception e) {
                log.error("Failed to initialize OpenAI provider: {}", e.getMessage(), e);
            }
        }

        // Initialize Anthropic provider if configured
        if (properties.getAnthropic().isEnabled() && properties.getAnthropic().getApiKey() != null) {
            try {
                AnthropicProvider anthropicProvider = new AnthropicProvider(
                        webClientBuilder,
                        properties.getAnthropic().getApiKey(),
                        asyncExecutor
                );
                providers.put(anthropicProvider.getProviderId(), anthropicProvider);
                log.info("Initialized Anthropic provider with {} models",
                        anthropicProvider.getAvailableModels().size());
            } catch (Exception e) {
                log.error("Failed to initialize Anthropic provider: {}", e.getMessage(), e);
            }
        }

        log.info("LLM Provider Factory initialized with {} providers", providers.size());
    }
}