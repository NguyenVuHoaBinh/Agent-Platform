package viettel.dac.promptservice.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import viettel.dac.promptservice.dto.request.PromptBatchTestRequest;
import viettel.dac.promptservice.dto.request.PromptTestRequest;
import viettel.dac.promptservice.dto.response.PromptExecutionResult;
import viettel.dac.promptservice.dto.validation.ValidationResult;
import viettel.dac.promptservice.exception.ResourceNotFoundException;
import viettel.dac.promptservice.model.enums.ExecutionStatus;
import viettel.dac.promptservice.service.preview.PromptTestingService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PromptTestingControllerTest {

    @Mock
    private PromptTestingService testingService;

    @InjectMocks
    private PromptTestingController testingController;

    private PromptTestRequest mockTestRequest;
    private PromptBatchTestRequest mockBatchTestRequest;
    private PromptExecutionResult mockExecutionResult;
    private List<PromptExecutionResult> mockExecutionResults;
    private ValidationResult mockValidationResult;
    private Map<String, Object> mockComparisonResult;

    @BeforeEach
    void setUp() {
        // Initialize mock data
        mockTestRequest = new PromptTestRequest();
        mockTestRequest.setVersionId("version-id-1");
        mockTestRequest.setProviderId("openai");
        mockTestRequest.setModelId("gpt-3.5-turbo");
        mockTestRequest.setParameters(new HashMap<>());

        mockBatchTestRequest = new PromptBatchTestRequest();
        mockBatchTestRequest.setVersionId("version-id-1");
        mockBatchTestRequest.setProviderId("openai");
        mockBatchTestRequest.setModelId("gpt-3.5-turbo");
        mockBatchTestRequest.setParameterSets(Arrays.asList(new HashMap<>()));

        mockExecutionResult = PromptExecutionResult.builder()
                .executionId("execution-id-1")
                .versionId("version-id-1")
                .templateId("template-id-1")
                .providerId("openai")
                .modelId("gpt-3.5-turbo")
                .response("Test response")
                .tokenCount(10)
                .inputTokens(5)
                .outputTokens(5)
                .cost(BigDecimal.valueOf(0.01))
                .responseTimeMs(500L)
                .executedAt(LocalDateTime.now())
                .executedBy("user-id-1")
                .status(ExecutionStatus.SUCCESS)
                .build();

        mockExecutionResults = Arrays.asList(mockExecutionResult);

        mockValidationResult = ValidationResult.builder()
                .passed(true)
                .score(1.0)
                .build();

        mockComparisonResult = new HashMap<>();
        mockComparisonResult.put("tokenCountDiff", 0);
        mockComparisonResult.put("responseTimeDiff", 0);
        mockComparisonResult.put("similarityScore", 1.0);
    }

    @Test
    void testPrompt_Success() {
        // Arrange
        when(testingService.testPrompt(any(PromptTestRequest.class))).thenReturn(mockExecutionResult);

        // Act
        ResponseEntity<PromptExecutionResult> response = testingController.testPrompt(mockTestRequest);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockExecutionResult, response.getBody());
        verify(testingService).testPrompt(mockTestRequest);
    }

    @Test
    void testPrompt_Failure() {
        // Arrange
        when(testingService.testPrompt(any(PromptTestRequest.class)))
                .thenThrow(new ResourceNotFoundException("Version not found"));

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> testingController.testPrompt(mockTestRequest));
        verify(testingService).testPrompt(mockTestRequest);
    }

    @Test
    void batchTestPrompt() {
        // Arrange
        when(testingService.batchTestPrompt(any(PromptBatchTestRequest.class))).thenReturn(mockExecutionResults);

        // Act
        ResponseEntity<List<PromptExecutionResult>> response = testingController.batchTestPrompt(mockBatchTestRequest);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockExecutionResults, response.getBody());
        verify(testingService).batchTestPrompt(mockBatchTestRequest);
    }

    @Test
    void validateResponse() {
        // Arrange
        String responseText = "This is a test response";
        Map<String, Object> validationCriteria = new HashMap<>();
        validationCriteria.put("contains", "test");

        when(testingService.validateResponse(anyString(), anyMap())).thenReturn(mockValidationResult);

        // Act
        ResponseEntity<ValidationResult> response = testingController.validateResponse(responseText, validationCriteria);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockValidationResult, response.getBody());
        verify(testingService).validateResponse(responseText, validationCriteria);
    }

    @Test
    void compareResponses() {
        // Arrange
        when(testingService.compareResponses(anyString(), anyString())).thenReturn(mockComparisonResult);

        // Act
        ResponseEntity<Map<String, Object>> response =
                testingController.compareResponses("execution-id-1", "execution-id-2");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockComparisonResult, response.getBody());
        verify(testingService).compareResponses("execution-id-1", "execution-id-2");
    }

    @Test
    void getTestHistory() {
        // Arrange
        when(testingService.getTestHistory(anyString(), anyInt())).thenReturn(mockExecutionResults);

        // Act
        ResponseEntity<List<PromptExecutionResult>> response = testingController.getTestHistory("version-id-1", 10);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockExecutionResults, response.getBody());
        verify(testingService).getTestHistory("version-id-1", 10);
    }

    @Test
    void getTestHistory_NotFound() {
        // Arrange
        when(testingService.getTestHistory(anyString(), anyInt()))
                .thenThrow(new ResourceNotFoundException("Version not found"));

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> testingController.getTestHistory("non-existent-id", 10));
        verify(testingService).getTestHistory("non-existent-id", 10);
    }
}