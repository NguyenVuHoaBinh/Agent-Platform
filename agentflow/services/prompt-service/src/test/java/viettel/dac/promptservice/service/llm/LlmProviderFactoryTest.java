package viettel.dac.promptservice.service.llm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import viettel.dac.promptservice.service.llm.providers.AnthropicProvider;
import viettel.dac.promptservice.service.llm.providers.OpenAiProvider;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LlmProviderFactoryTest {

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private Executor executor;

    private LlmProviderProperties properties;
    private LlmProviderFactory factory;

    @BeforeEach
    void setUp() {
        // Create provider properties with test values
        properties = new LlmProviderProperties();

        // Configure OpenAI provider
        properties.getOpenai().setEnabled(true);
        properties.getOpenai().setApiKey("openai-test-key");

        // Configure Anthropic provider
        properties.getAnthropic().setEnabled(true);
        properties.getAnthropic().setApiKey("anthropic-test-key");

        // Create the factory
        factory = new LlmProviderFactory(webClientBuilder, executor, properties);
    }

    @Test
    void testGetProvider_OpenAi() {
        // When
        Optional<LlmProvider> provider = factory.getProvider("openai");

        // Then
        assertTrue(provider.isPresent());
        assertEquals("openai", provider.get().getProviderId());
        assertTrue(provider.get() instanceof OpenAiProvider);
    }

    @Test
    void testGetProvider_Anthropic() {
        // When
        Optional<LlmProvider> provider = factory.getProvider("anthropic");

        // Then
        assertTrue(provider.isPresent());
        assertEquals("anthropic", provider.get().getProviderId());
        assertTrue(provider.get() instanceof AnthropicProvider);
    }

    @Test
    void testGetProvider_Unknown() {
        // When
        Optional<LlmProvider> provider = factory.getProvider("unknown");

        // Then
        assertFalse(provider.isPresent());
    }

    @Test
    void testGetAllProviders() {
        // When
        Map<String, LlmProvider> providers = factory.getAllProviders();

        // Then
        assertNotNull(providers);
        assertEquals(2, providers.size());
        assertTrue(providers.containsKey("openai"));
        assertTrue(providers.containsKey("anthropic"));
    }

    @Test
    void testHasProvider() {
        // Then
        assertTrue(factory.hasProvider("openai"));
        assertTrue(factory.hasProvider("anthropic"));
        assertFalse(factory.hasProvider("unknown"));
    }

    @Test
    void testGetProviderForModel_OpenAiModel() {
        // When
        Optional<LlmProvider> provider = factory.getProviderForModel("gpt-4");

        // Then
        assertTrue(provider.isPresent());
        assertEquals("openai", provider.get().getProviderId());
    }

    @Test
    void testGetProviderForModel_AnthropicModel() {
        // When
        Optional<LlmProvider> provider = factory.getProviderForModel("claude-3-sonnet-20240229");

        // Then
        assertTrue(provider.isPresent());
        assertEquals("anthropic", provider.get().getProviderId());
    }

    @Test
    void testGetProviderForModel_UnknownModel() {
        // When
        Optional<LlmProvider> provider = factory.getProviderForModel("unknown-model");

        // Then
        assertFalse(provider.isPresent());
    }

    @Test
    void testInitializeProviders_Disabled() {
        // Given
        LlmProviderProperties disabledProperties = new LlmProviderProperties();
        disabledProperties.getOpenai().setEnabled(false);
        disabledProperties.getAnthropic().setEnabled(false);

        // When
        LlmProviderFactory disabledFactory = new LlmProviderFactory(webClientBuilder, executor, disabledProperties);

        // Then
        assertTrue(disabledFactory.getAllProviders().isEmpty());
    }

    @Test
    void testInitializeProviders_MissingApiKey() {
        // Given
        LlmProviderProperties missingKeyProperties = new LlmProviderProperties();
        missingKeyProperties.getOpenai().setEnabled(true);
        missingKeyProperties.getOpenai().setApiKey(null); // Missing API key

        // When
        LlmProviderFactory missingKeyFactory = new LlmProviderFactory(webClientBuilder, executor, missingKeyProperties);

        // Then
        assertFalse(missingKeyFactory.hasProvider("openai"));
    }
}