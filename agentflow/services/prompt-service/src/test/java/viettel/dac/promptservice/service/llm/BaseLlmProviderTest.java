package viettel.dac.promptservice.service.llm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import viettel.dac.promptservice.dto.request.LlmRequest;
import viettel.dac.promptservice.dto.response.LlmResponse;
import viettel.dac.promptservice.exception.LlmProviderException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BaseLlmProviderTest {

    @Mock
    private Executor executor;

    private TestLlmProvider provider;
    private LlmRequest validRequest;

    // Test implementation of BaseLlmProvider
    private static class TestLlmProvider extends BaseLlmProvider {
        private final Map<String, String> models;
        private final boolean shouldThrowException;
        private final LlmProviderException.ErrorType errorType;
        private final int maxContextLength;

        public TestLlmProvider(Executor executor, Map<String, String> models, boolean shouldThrowException,
                               LlmProviderException.ErrorType errorType, int maxContextLength) {
            super(executor);
            this.models = models;
            this.shouldThrowException = shouldThrowException;
            this.errorType = errorType;
            this.maxContextLength = maxContextLength;
        }

        @Override
        public String getProviderId() {
            return "test-provider";
        }

        @Override
        public Map<String, String> getAvailableModels() {
            return models;
        }

        @Override
        public LlmResponse executePrompt(LlmRequest request) throws LlmProviderException {
            if (shouldThrowException) {
                throw new LlmProviderException("Test exception", getProviderId(), request.getModelId(), errorType);
            }

            return LlmResponse.builder()
                    .text("Test response")
                    .inputTokenCount(10)
                    .outputTokenCount(20)
                    .totalTokenCount(30)
                    .cost(0.001)
                    .successful(true)
                    .build();
        }

        @Override
        public int countTokens(String prompt, String modelId) {
            return prompt.length() / 4; // Simple approximation
        }

        @Override
        public double calculateCost(int inputTokens, int outputTokens, String modelId) {
            return (inputTokens + outputTokens) * 0.00001;
        }

        @Override
        public int getMaxContextLength(String modelId) {
            return maxContextLength;
        }
    }

    @BeforeEach
    void setUp() {
        // Create a provider with a valid model
        provider = new TestLlmProvider(
                executor,
                Map.of("test-model", "Test Model"),
                false,
                null,
                1000
        );

        // Create a valid request
        validRequest = LlmRequest.builder()
                .providerId("test-provider")
                .modelId("test-model")
                .prompt("This is a test prompt")
                .timeoutMs(1000)
                .build();

        // Set up executor to execute tasks immediately
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(executor).execute(any(Runnable.class));
    }

    @Test
    void testExecutePromptAsync_Success() {
        // When
        CompletableFuture<LlmResponse> future = provider.executePromptAsync(validRequest);

        // Then
        assertTrue(future.isDone());
        assertFalse(future.isCompletedExceptionally());

        LlmResponse response = future.join();
        assertEquals("Test response", response.getText());
        assertEquals(30, response.getTotalTokenCount());
    }

    @Test
    void testExecutePromptAsync_Exception() {
        // Given
        provider = new TestLlmProvider(
                executor,
                Map.of("test-model", "Test Model"),
                true,
                LlmProviderException.ErrorType.INVALID_REQUEST,
                1000
        );

        // When
        CompletableFuture<LlmResponse> future = provider.executePromptAsync(validRequest);

        // Then
        assertTrue(future.isDone());
        assertTrue(future.isCompletedExceptionally());

        Exception exception = assertThrows(LlmProviderException.class, future::join);
        assertTrue(exception instanceof LlmProviderException);
        LlmProviderException llmException = (LlmProviderException) exception;
        assertEquals(LlmProviderException.ErrorType.INVALID_REQUEST, llmException.getErrorType());
    }

    @Test
    void testExecutePromptAsync_Timeout() throws Exception {
        // Given
        // Create a provider that will delay execution
        provider = spy(new TestLlmProvider(
                executor,
                Map.of("test-model", "Test Model"),
                false,
                null,
                1000
        ));

        // Make the execution delay longer than the timeout
        doAnswer(invocation -> {
            Thread.sleep(100); // Delay execution
            return invocation.callRealMethod();
        }).when(provider).executePrompt(any());

        // Create a request with a very short timeout
        LlmRequest request = LlmRequest.builder()
                .providerId("test-provider")
                .modelId("test-model")
                .prompt("This is a test prompt")
                .timeoutMs(50) // Very short timeout
                .build();

        // When
        CompletableFuture<LlmResponse> future = provider.executePromptAsync(request);

        // Then
        Exception exception = assertThrows(Exception.class, () -> future.get(200, TimeUnit.MILLISECONDS));
        assertTrue(exception.getCause() instanceof LlmProviderException);
        LlmProviderException llmException = (LlmProviderException) exception.getCause();
        assertEquals(LlmProviderException.ErrorType.TIMEOUT, llmException.getErrorType());
    }

    @Test
    void testValidateRequest_ValidRequest() {
        // When & Then - No exception should be thrown
        assertDoesNotThrow(() -> provider.validateRequest(validRequest));
    }

    @Test
    void testValidateRequest_NullRequest() {
        // When & Then
        LlmProviderException exception = assertThrows(LlmProviderException.class,
                () -> provider.validateRequest(null));
        assertEquals(LlmProviderException.ErrorType.INVALID_REQUEST, exception.getErrorType());
    }

    @Test
    void testValidateRequest_EmptyPrompt() {
        // Given
        LlmRequest request = LlmRequest.builder()
                .providerId("test-provider")
                .modelId("test-model")
                .prompt("")
                .build();

        // When & Then
        LlmProviderException exception = assertThrows(LlmProviderException.class,
                () -> provider.validateRequest(request));
        assertEquals(LlmProviderException.ErrorType.INVALID_REQUEST, exception.getErrorType());
    }

    @Test
    void testValidateRequest_UnsupportedModel() {
        // Given
        LlmRequest request = LlmRequest.builder()
                .providerId("test-provider")
                .modelId("unsupported-model")
                .prompt("This is a test prompt")
                .build();

        // When & Then
        LlmProviderException exception = assertThrows(LlmProviderException.class,
                () -> provider.validateRequest(request));
        assertEquals(LlmProviderException.ErrorType.INVALID_REQUEST, exception.getErrorType());
    }

    @Test
    void testValidateRequest_ExceedsContextLength() {
        // Given
        // Create a request with a prompt that exceeds max context length
        LlmRequest request = LlmRequest.builder()
                .providerId("test-provider")
                .modelId("test-model")
                .prompt("X".repeat(5000)) // Create a very long prompt
                .build();

        // When & Then
        LlmProviderException exception = assertThrows(LlmProviderException.class,
                () -> provider.validateRequest(request));
        assertEquals(LlmProviderException.ErrorType.CONTEXT_LENGTH, exception.getErrorType());
    }

    @Test
    void testCreateBaseResponse() {
        // Given
        LocalDateTime startTime = LocalDateTime.now().minusSeconds(1);

        // When
        LlmResponse.LlmResponseBuilder responseBuilder = provider.createBaseResponse(validRequest, startTime);
        LlmResponse response = responseBuilder.text("Test").build();

        // Then
        assertEquals(validRequest, response.getRequest());
        assertEquals(startTime, response.getStartTime());
        assertNotNull(response.getCompletionTime());
        assertNotNull(response.getResponseTimeMs());
        assertTrue(response.getResponseTimeMs() > 0);
    }
}