package viettel.dac.promptservice.service.llm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import viettel.dac.promptservice.dto.request.LlmRequest;
import viettel.dac.promptservice.dto.response.LlmResponse;
import viettel.dac.promptservice.exception.LlmProviderException;
import viettel.dac.promptservice.exception.ValidationException;
import viettel.dac.promptservice.model.entity.PromptExecution;
import viettel.dac.promptservice.model.entity.PromptParameter;
import viettel.dac.promptservice.model.entity.PromptTemplate;
import viettel.dac.promptservice.model.entity.PromptVersion;
import viettel.dac.promptservice.model.enums.ExecutionStatus;
import viettel.dac.promptservice.model.enums.ParameterType;
import viettel.dac.promptservice.repository.jpa.PromptExecutionRepository;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LlmServiceTest {

    @Mock
    private LlmProviderFactory providerFactory;

    @Mock
    private LlmProviderProperties providerProperties;

    @Mock
    private PromptExecutionRepository executionRepository;

    @Mock
    private LlmProvider llmProvider;

    @InjectMocks
    private LlmService llmService;

    // Test data
    private final String PROVIDER_ID = "openai";
    private final String MODEL_ID = "gpt-3.5-turbo";
    private final String VERSION_ID = "version-123";
    private final String TEMPLATE_ID = "template-123";
    private PromptVersion testVersion;
    private Map<String, Object> parameters;
    private LlmResponse successResponse;

    @BeforeEach
    void setUp() {
        // Setup test template
        PromptTemplate testTemplate = PromptTemplate.builder()
                .id(TEMPLATE_ID)
                .name("Test Template")
                .build();

        // Setup test version
        testVersion = PromptVersion.builder()
                .id(VERSION_ID)
                .template(testTemplate)
                .versionNumber("1.0.0")
                .content("This is a test prompt with {{parameter}}")
                .systemPrompt("You are a helpful assistant.")
                .build();

        // Add parameter to version
        PromptParameter parameter = PromptParameter.builder()
                .name("parameter")
                .parameterType(ParameterType.STRING)
                .required(true)
                .build();
        testVersion.addParameter(parameter);

        // Setup parameters
        parameters = new HashMap<>();
        parameters.put("parameter", "test value");

        // Setup success response
        successResponse = LlmResponse.builder()
                .text("This is a generated response")
                .inputTokenCount(10)
                .outputTokenCount(20)
                .totalTokenCount(30)
                .cost(0.002)
                .responseTimeMs(500L)
                .successful(true)
                .build();

        // Setup provider properties defaults
        when(providerProperties.getDefaultMaxTokens()).thenReturn(1024);
        when(providerProperties.getDefaultTemperature()).thenReturn(0.7);
        when(providerProperties.getDefaultTimeoutMs()).thenReturn(30000);
    }

    @Test
    @DisplayName("Should execute prompt successfully")
    void shouldExecutePromptSuccessfully() throws LlmProviderException {
        // Arrange
        when(providerFactory.getProvider(PROVIDER_ID)).thenReturn(Optional.of(llmProvider));
        when(llmProvider.executePrompt(any(LlmRequest.class))).thenReturn(successResponse);
        when(executionRepository.save(any(PromptExecution.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        PromptExecution result = llmService.executePrompt(testVersion, PROVIDER_ID, MODEL_ID, parameters);

        // Assert
        assertNotNull(result);
        assertEquals(VERSION_ID, result.getVersion().getId());
        assertEquals(PROVIDER_ID, result.getProviderId());
        assertEquals(MODEL_ID, result.getModelId());
        assertEquals("This is a generated response", result.getRawResponse());
        assertEquals(30, result.getTokenCount());
        assertEquals(10, result.getInputTokens());
        assertEquals(20, result.getOutputTokens());
        assertEquals(BigDecimal.valueOf(0.002), result.getCost());
        assertEquals(500L, result.getResponseTimeMs());
        assertEquals(ExecutionStatus.SUCCESS, result.getStatus());

        // Verify provider interactions
        verify(providerFactory).getProvider(PROVIDER_ID);
        verify(llmProvider).executePrompt(any(LlmRequest.class));
        verify(executionRepository).save(any(PromptExecution.class));

        // Verify request parameters
        ArgumentCaptor<LlmRequest> requestCaptor = ArgumentCaptor.forClass(LlmRequest.class);
        verify(llmProvider).executePrompt(requestCaptor.capture());
        LlmRequest capturedRequest = requestCaptor.getValue();

        assertEquals(PROVIDER_ID, capturedRequest.getProviderId());
        assertEquals(MODEL_ID, capturedRequest.getModelId());
        assertEquals("This is a test prompt with test value", capturedRequest.getPrompt());
        assertEquals("You are a helpful assistant.", capturedRequest.getSystemPrompt());
    }

    @Test
    @DisplayName("Should handle provider not found")
    void shouldHandleProviderNotFound() {
        // Arrange
        when(providerFactory.getProvider("invalid-provider")).thenReturn(Optional.empty());
        when(executionRepository.save(any(PromptExecution.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        // The service catches the LlmProviderException rather than letting it propagate
        PromptExecution result = llmService.executePrompt(testVersion, "invalid-provider", MODEL_ID, parameters);

        // Assert
        assertNotNull(result);
        assertEquals(ExecutionStatus.INVALID_PARAMS, result.getStatus());
        assertTrue(result.getRawResponse().contains("Provider not found"));

        // Verify interactions
        verify(providerFactory).getProvider("invalid-provider");
        verify(executionRepository).save(any(PromptExecution.class));
    }

    @Test
    @DisplayName("Should apply parameters to prompt")
    void shouldApplyParametersToPrompt() throws LlmProviderException {
        // Arrange
        when(providerFactory.getProvider(PROVIDER_ID)).thenReturn(Optional.of(llmProvider));
        when(llmProvider.executePrompt(any(LlmRequest.class))).thenReturn(successResponse);
        when(executionRepository.save(any(PromptExecution.class))).thenAnswer(i -> i.getArgument(0));

        // Custom parameters
        Map<String, Object> customParams = new HashMap<>();
        customParams.put("parameter", "custom value");

        // Act
        llmService.executePrompt(testVersion, PROVIDER_ID, MODEL_ID, customParams);

        // Verify parameters were applied to prompt
        ArgumentCaptor<LlmRequest> requestCaptor = ArgumentCaptor.forClass(LlmRequest.class);
        verify(llmProvider).executePrompt(requestCaptor.capture());

        // Check that parameter was substituted
        assertEquals("This is a test prompt with custom value", requestCaptor.getValue().getPrompt());
    }

    @Test
    @DisplayName("Should record execution metrics")
    void shouldRecordExecutionMetrics() throws LlmProviderException {
        // Arrange
        LlmResponse customResponse = LlmResponse.builder()
                .text("Custom response")
                .inputTokenCount(15)
                .outputTokenCount(25)
                .totalTokenCount(40)
                .cost(0.003)
                .responseTimeMs(600L)
                .successful(true)
                .build();

        when(providerFactory.getProvider(PROVIDER_ID)).thenReturn(Optional.of(llmProvider));
        when(llmProvider.executePrompt(any(LlmRequest.class))).thenReturn(customResponse);
        when(executionRepository.save(any(PromptExecution.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        PromptExecution result = llmService.executePrompt(testVersion, PROVIDER_ID, MODEL_ID, parameters);

        // Assert metrics
        assertEquals(40, result.getTokenCount());
        assertEquals(15, result.getInputTokens());
        assertEquals(25, result.getOutputTokens());
        assertEquals(BigDecimal.valueOf(0.003), result.getCost());
        assertEquals(600L, result.getResponseTimeMs());
    }

    @Test
    @DisplayName("Should map provider error to execution status")
    void shouldMapProviderErrorToExecutionStatus() throws LlmProviderException {
        // Arrange
        LlmProviderException providerException = new LlmProviderException(
                "Rate limit exceeded",
                PROVIDER_ID, MODEL_ID, LlmProviderException.ErrorType.RATE_LIMIT);

        when(providerFactory.getProvider(PROVIDER_ID)).thenReturn(Optional.of(llmProvider));
        when(llmProvider.executePrompt(any(LlmRequest.class))).thenThrow(providerException);
        when(executionRepository.save(any(PromptExecution.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        PromptExecution result = llmService.executePrompt(testVersion, PROVIDER_ID, MODEL_ID, parameters);

        // Assert
        assertEquals(ExecutionStatus.RATE_LIMITED, result.getStatus());
        assertTrue(result.getRawResponse().contains("Error:"));
        assertTrue(result.getRawResponse().contains("Rate limit exceeded"));
    }

    @Test
    @DisplayName("Should get available providers")
    void shouldGetAvailableProviders() {
        // Arrange
        Map<String, List<String>> providerModels = new HashMap<>();
        providerModels.put("openai", Arrays.asList("gpt-3.5-turbo", "gpt-4"));
        providerModels.put("anthropic", Arrays.asList("claude-1", "claude-2"));

        Map<String, String> openaiModels = new HashMap<>();
        openaiModels.put("gpt-3.5-turbo", "GPT-3.5 Turbo");
        openaiModels.put("gpt-4", "GPT-4");

        Map<String, String> anthropicModels = new HashMap<>();
        anthropicModels.put("claude-1", "Claude 1");
        anthropicModels.put("claude-2", "Claude 2");

        when(llmProvider.getProviderId()).thenReturn("openai");
        when(llmProvider.getAvailableModels()).thenReturn(openaiModels);

        LlmProvider anthropicProvider = mock(LlmProvider.class);
        when(anthropicProvider.getProviderId()).thenReturn("anthropic");
        when(anthropicProvider.getAvailableModels()).thenReturn(anthropicModels);

        Map<String, LlmProvider> providers = new HashMap<>();
        providers.put("openai", llmProvider);
        providers.put("anthropic", anthropicProvider);

        when(providerFactory.getAllProviders()).thenReturn(providers);

        // Act
        Map<String, List<String>> result = llmService.getAvailableProviders();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.containsKey("openai"));
        assertTrue(result.containsKey("anthropic"));
        assertEquals(2, result.get("openai").size());
        assertEquals(2, result.get("anthropic").size());
    }

    @Test
    @DisplayName("Should get available models for provider")
    void shouldGetAvailableModelsForProvider() {
        // Arrange
        Map<String, String> openaiModels = new HashMap<>();
        openaiModels.put("gpt-3.5-turbo", "GPT-3.5 Turbo");
        openaiModels.put("gpt-4", "GPT-4");

        when(llmProvider.getAvailableModels()).thenReturn(openaiModels);
        when(providerFactory.getProvider(PROVIDER_ID)).thenReturn(Optional.of(llmProvider));

        // Act
        Map<String, String> result = llmService.getAvailableModels(PROVIDER_ID);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("GPT-3.5 Turbo", result.get("gpt-3.5-turbo"));
        assertEquals("GPT-4", result.get("gpt-4"));
    }

    @Test
    @DisplayName("Should return empty map for unknown provider")
    void shouldReturnEmptyMapForUnknownProvider() {
        // Arrange
        when(providerFactory.getProvider("unknown")).thenReturn(Optional.empty());

        // Act
        Map<String, String> result = llmService.getAvailableModels("unknown");

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should check if provider is available")
    void shouldCheckIfProviderIsAvailable() {
        // Arrange
        when(providerFactory.hasProvider(PROVIDER_ID)).thenReturn(true);
        when(providerFactory.hasProvider("unknown")).thenReturn(false);

        // Act & Assert
        assertTrue(llmService.isProviderAvailable(PROVIDER_ID));
        assertFalse(llmService.isProviderAvailable("unknown"));
    }

    @Test
    @DisplayName("Should handle different error types from provider")
    void shouldHandleDifferentErrorTypesFromProvider() throws LlmProviderException {
        // Arrange
        when(providerFactory.getProvider(PROVIDER_ID)).thenReturn(Optional.of(llmProvider));
        when(executionRepository.save(any(PromptExecution.class))).thenAnswer(i -> i.getArgument(0));

        // Setup error types to test
        Map<LlmProviderException.ErrorType, ExecutionStatus> errorTypeToStatus = new HashMap<>();
        errorTypeToStatus.put(LlmProviderException.ErrorType.AUTHENTICATION, ExecutionStatus.PROVIDER_ERROR);
        errorTypeToStatus.put(LlmProviderException.ErrorType.RATE_LIMIT, ExecutionStatus.RATE_LIMITED);
        errorTypeToStatus.put(LlmProviderException.ErrorType.CONTEXT_LENGTH, ExecutionStatus.INVALID_PARAMS);
        errorTypeToStatus.put(LlmProviderException.ErrorType.CONTENT_FILTER, ExecutionStatus.PROVIDER_ERROR);
        errorTypeToStatus.put(LlmProviderException.ErrorType.INVALID_REQUEST, ExecutionStatus.INVALID_PARAMS);
        errorTypeToStatus.put(LlmProviderException.ErrorType.SERVICE_UNAVAILABLE, ExecutionStatus.PROVIDER_ERROR);
        errorTypeToStatus.put(LlmProviderException.ErrorType.TIMEOUT, ExecutionStatus.TIMEOUT);
        errorTypeToStatus.put(LlmProviderException.ErrorType.UNKNOWN, ExecutionStatus.ERROR);

        // Test each error type
        for (Map.Entry<LlmProviderException.ErrorType, ExecutionStatus> entry : errorTypeToStatus.entrySet()) {
            // Reset mocks for each test
            reset(llmProvider);

            LlmProviderException exception = new LlmProviderException(
                    "Error: " + entry.getKey().name(),
                    PROVIDER_ID, MODEL_ID, entry.getKey());

            when(llmProvider.executePrompt(any(LlmRequest.class))).thenThrow(exception);

            // Act
            PromptExecution result = llmService.executePrompt(testVersion, PROVIDER_ID, MODEL_ID, parameters);

            // Assert
            assertEquals(entry.getValue(), result.getStatus());
        }
    }

    @Test
    @DisplayName("Should execute prompt asynchronously")
    void shouldExecutePromptAsynchronously() throws Exception {
        // Arrange
        when(providerFactory.getProvider(PROVIDER_ID)).thenReturn(Optional.of(llmProvider));
        when(llmProvider.executePrompt(any(LlmRequest.class))).thenReturn(successResponse);
        when(executionRepository.save(any(PromptExecution.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        CompletableFuture<PromptExecution> futureResult = llmService.executePromptAsync(
                testVersion, PROVIDER_ID, MODEL_ID, parameters);

        // Wait for completion
        PromptExecution result = futureResult.get();

        // Assert
        assertNotNull(result);
        assertEquals(ExecutionStatus.SUCCESS, result.getStatus());
        assertEquals("This is a generated response", result.getRawResponse());
    }

    @Test
    @DisplayName("Should use custom system prompt when provided")
    void shouldUseCustomSystemPromptWhenProvided() throws LlmProviderException {
        // Arrange
        PromptVersion versionWithCustomPrompt = PromptVersion.builder()
                .id(VERSION_ID)
                .template(testVersion.getTemplate())
                .versionNumber("1.0.0")
                .content("Test prompt")
                .systemPrompt("Custom system prompt for testing")
                .build();

        when(providerFactory.getProvider(PROVIDER_ID)).thenReturn(Optional.of(llmProvider));
        when(llmProvider.executePrompt(any(LlmRequest.class))).thenReturn(successResponse);
        when(executionRepository.save(any(PromptExecution.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        llmService.executePrompt(versionWithCustomPrompt, PROVIDER_ID, MODEL_ID, parameters);

        // Verify system prompt was used
        ArgumentCaptor<LlmRequest> requestCaptor = ArgumentCaptor.forClass(LlmRequest.class);
        verify(llmProvider).executePrompt(requestCaptor.capture());

        assertEquals("Custom system prompt for testing", requestCaptor.getValue().getSystemPrompt());
    }

    @Test
    @DisplayName("Should handle null parameters")
    void shouldHandleNullParameters() {
        // Arrange
        when(providerFactory.getProvider(PROVIDER_ID)).thenReturn(Optional.of(llmProvider));
        when(executionRepository.save(any(PromptExecution.class))).thenAnswer(i -> i.getArgument(0));

        // The test version has a required parameter, so null parameters will throw an IllegalArgumentException
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> {
                // This ensures the provider factory is called before the exception
                llmService.executePrompt(testVersion, PROVIDER_ID, MODEL_ID, null);
            }
        );
        
        // Verify the exception message contains information about the missing parameter
        assertTrue(exception.getMessage().contains("Missing required parameters"));
        assertTrue(exception.getMessage().contains("parameter"));
        
        // The LLM provider shouldn't be called since we throw an exception before that
        verify(llmProvider, never()).executePrompt(any(LlmRequest.class));
    }
}