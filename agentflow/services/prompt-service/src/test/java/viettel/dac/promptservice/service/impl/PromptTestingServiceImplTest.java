package viettel.dac.promptservice.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import viettel.dac.promptservice.dto.request.LlmRequest;
import viettel.dac.promptservice.dto.request.PromptBatchTestRequest;
import viettel.dac.promptservice.dto.request.PromptTestRequest;
import viettel.dac.promptservice.dto.response.LlmResponse;
import viettel.dac.promptservice.dto.response.PromptExecutionResult;
import viettel.dac.promptservice.dto.validation.ParameterValidationResult;
import viettel.dac.promptservice.dto.validation.ValidationResult;
import viettel.dac.promptservice.exception.ResourceNotFoundException;
import viettel.dac.promptservice.model.entity.PromptExecution;
import viettel.dac.promptservice.model.entity.PromptParameter;
import viettel.dac.promptservice.model.entity.PromptTemplate;
import viettel.dac.promptservice.model.entity.PromptVersion;
import viettel.dac.promptservice.model.enums.ExecutionStatus;
import viettel.dac.promptservice.model.enums.ParameterType;
import viettel.dac.promptservice.model.enums.VersionStatus;
import viettel.dac.promptservice.repository.jpa.PromptExecutionRepository;
import viettel.dac.promptservice.repository.jpa.PromptVersionRepository;
import viettel.dac.promptservice.security.SecurityUtils;
import viettel.dac.promptservice.service.llm.LlmProvider;
import viettel.dac.promptservice.service.llm.LlmProviderFactory;
import viettel.dac.promptservice.service.llm.LlmProviderProperties;
import viettel.dac.promptservice.service.mapper.EntityDtoMapper;
import viettel.dac.promptservice.service.validation.ParameterValidator;
import viettel.dac.promptservice.service.validation.ResponseValidator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import org.mockito.junit.jupiter.MockitoSettings;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class PromptTestingServiceImplTest {

    @Mock
    private PromptVersionRepository versionRepository;

    @Mock
    private PromptExecutionRepository executionRepository;

    @Mock
    private LlmProviderFactory providerFactory;

    @Mock
    private LlmProviderProperties providerProperties;

    @Mock
    private ParameterValidator parameterValidator;

    @Mock
    private ResponseValidator responseValidator;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private EntityDtoMapper mapper;

    @Mock
    private LlmProvider llmProvider;

    @InjectMocks
    private PromptTestingServiceImpl testingService;

    // Test data
    private final String VERSION_ID = "version-123";
    private final String PROVIDER_ID = "openai";
    private final String MODEL_ID = "gpt-3.5-turbo";
    private final String USER_ID = "user-123";
    private final String TEMPLATE_ID = "template-123";
    private final String EXECUTION_ID = "execution-123";

    private PromptVersion testVersion;
    private PromptTestRequest testRequest;
    private Map<String, Object> validParameters;
    private ParameterValidationResult validParameterResult;
    private LlmResponse llmResponse;
    private PromptExecution savedExecution;

    @BeforeEach
    void setUp() {
        // Setup template
        PromptTemplate testTemplate = PromptTemplate.builder()
                .id(TEMPLATE_ID)
                .name("Test Template")
                .build();

        // Setup version
        testVersion = PromptVersion.builder()
                .id(VERSION_ID)
                .template(testTemplate)
                .versionNumber("1.0.0")
                .content("This is a test prompt with {{parameter}}")
                .status(VersionStatus.PUBLISHED)
                .createdBy(USER_ID)
                .build();

        // Add parameter to version
        PromptParameter parameter = PromptParameter.builder()
                .name("parameter")
                .parameterType(ParameterType.STRING)
                .required(true)
                .build();
        testVersion.addParameter(parameter);

        // Setup valid parameters
        validParameters = new HashMap<>();
        validParameters.put("parameter", "test value");

        // Setup valid parameter validation result
        validParameterResult = ParameterValidationResult.builder()
                .valid(true)
                .validatedValues(validParameters)
                .build();

        // Setup test request
        testRequest = PromptTestRequest.builder()
                .versionId(VERSION_ID)
                .providerId(PROVIDER_ID)
                .modelId(MODEL_ID)
                .parameters(validParameters)
                .maxTokens(100)
                .temperature(0.7)
                .storeResult(true)
                .build();

        // Setup LLM response
        llmResponse = LlmResponse.builder()
                .text("This is a generated response")
                .inputTokenCount(10)
                .outputTokenCount(20)
                .totalTokenCount(30)
                .cost(0.002)
                .responseTimeMs(500L)
                .successful(true)
                .build();

        // Setup saved execution
        savedExecution = PromptExecution.builder()
                .id(EXECUTION_ID)
                .version(testVersion)
                .providerId(PROVIDER_ID)
                .modelId(MODEL_ID)
                .inputParameters(validParameters)
                .rawResponse("This is a generated response")
                .tokenCount(30)
                .inputTokens(10)
                .outputTokens(20)
                .cost(BigDecimal.valueOf(0.002))
                .responseTimeMs(500L)
                .executedAt(LocalDateTime.now())
                .executedBy(USER_ID)
                .status(ExecutionStatus.SUCCESS)
                .build();

        // Setup security utils
        when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(USER_ID));

        // Setup provider properties defaults
        when(providerProperties.getDefaultMaxTokens()).thenReturn(1024);
        when(providerProperties.getDefaultTemperature()).thenReturn(0.7);
        when(providerProperties.getDefaultTimeoutMs()).thenReturn(30000);
    }

    @Test
    @DisplayName("Should test prompt successfully")
    void shouldTestPromptSuccessfully() {
        // Arrange
        when(versionRepository.findByIdWithParameters(VERSION_ID)).thenReturn(Optional.of(testVersion));
        when(parameterValidator.validateParameters(eq(testVersion), anyMap())).thenReturn(validParameterResult);
        when(providerFactory.getProvider(PROVIDER_ID)).thenReturn(Optional.of(llmProvider));
        when(llmProvider.executePrompt(any(LlmRequest.class))).thenReturn(llmResponse);
        when(executionRepository.save(any(PromptExecution.class))).thenReturn(savedExecution);

        // Act
        PromptExecutionResult result = testingService.testPrompt(testRequest);

        // Assert
        assertNotNull(result);
        assertEquals(VERSION_ID, result.getVersionId());
        assertEquals(PROVIDER_ID, result.getProviderId());
        assertEquals(MODEL_ID, result.getModelId());
        assertEquals("This is a generated response", result.getResponse());
        assertEquals(30, result.getTokenCount());
        assertEquals(ExecutionStatus.SUCCESS, result.getStatus());
        assertEquals(EXECUTION_ID, result.getExecutionId());

        // Verify interactions
        verify(versionRepository).findByIdWithParameters(VERSION_ID);
        verify(parameterValidator).validateParameters(eq(testVersion), anyMap());
        verify(providerFactory).getProvider(PROVIDER_ID);
        verify(llmProvider).executePrompt(any(LlmRequest.class));
        verify(executionRepository).save(any(PromptExecution.class));
    }

    @Test
    @DisplayName("Should handle parameter validation failure")
    void shouldHandleParameterValidationFailure() {
        // Arrange
        ParameterValidationResult invalidResult = ParameterValidationResult.builder()
                .valid(false)
                .issues(Collections.singletonList(
                        ParameterValidationResult.ValidationIssue.builder()
                                .parameter("parameter")
                                .message("Required parameter is missing")
                                .severity(ParameterValidationResult.ValidationSeverity.ERROR)
                                .build()
                ))
                .missingRequired(Collections.singletonList("parameter"))
                .build();

        when(versionRepository.findByIdWithParameters(VERSION_ID)).thenReturn(Optional.of(testVersion));
        when(parameterValidator.validateParameters(eq(testVersion), anyMap())).thenReturn(invalidResult);
        when(executionRepository.save(any(PromptExecution.class))).thenReturn(savedExecution);

        // Act
        PromptExecutionResult result = testingService.testPrompt(testRequest);

        // Assert
        assertNotNull(result);
        assertEquals(ExecutionStatus.INVALID_PARAMS, result.getStatus());
        assertTrue(result.getResponse().contains("Error:"));
        assertTrue(result.getResponse().contains("Parameter validation failed"));

        // Verify no LLM call was made
        verify(llmProvider, never()).executePrompt(any(LlmRequest.class));

        // Verify execution was still saved (with error status)
        verify(executionRepository).save(any(PromptExecution.class));
    }

    @Test
    @DisplayName("Should throw exception when version not found")
    void shouldThrowExceptionWhenVersionNotFound() {
        // Arrange
        when(versionRepository.findByIdWithParameters("non-existent"))
                .thenReturn(Optional.empty());

        PromptTestRequest request = PromptTestRequest.builder()
                .versionId("non-existent")
                .providerId(PROVIDER_ID)
                .modelId(MODEL_ID)
                .build();

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> testingService.testPrompt(request));

        // Verify no execution was saved
        verify(executionRepository, never()).save(any(PromptExecution.class));
    }

    @Test
    @DisplayName("Should not store execution when not requested")
    void shouldNotStoreExecutionWhenNotRequested() {
        // Arrange
        testRequest.setStoreResult(false);

        when(versionRepository.findByIdWithParameters(VERSION_ID)).thenReturn(Optional.of(testVersion));
        when(parameterValidator.validateParameters(eq(testVersion), anyMap())).thenReturn(validParameterResult);
        when(providerFactory.getProvider(PROVIDER_ID)).thenReturn(Optional.of(llmProvider));
        when(llmProvider.executePrompt(any(LlmRequest.class))).thenReturn(llmResponse);

        // Act
        PromptExecutionResult result = testingService.testPrompt(testRequest);

        // Assert
        assertNotNull(result);
        assertNull(result.getExecutionId());

        // Verify no execution was saved
        verify(executionRepository, never()).save(any(PromptExecution.class));
    }

    @Test
    @DisplayName("Should validate response against criteria and pass")
    void shouldValidateResponseAgainstCriteriaAndPass() {
        // Arrange
        Map<String, Object> validationCriteria = new HashMap<>();
        validationCriteria.put("contains", "expected text");

        ValidationResult validationResult = ValidationResult.builder()
                .passed(true)
                .score(1.0)
                .build();

        testRequest.setValidationCriteria(validationCriteria);

        when(versionRepository.findByIdWithParameters(VERSION_ID)).thenReturn(Optional.of(testVersion));
        when(parameterValidator.validateParameters(eq(testVersion), anyMap())).thenReturn(validParameterResult);
        when(providerFactory.getProvider(PROVIDER_ID)).thenReturn(Optional.of(llmProvider));
        when(llmProvider.executePrompt(any(LlmRequest.class))).thenReturn(llmResponse);
        when(executionRepository.save(any(PromptExecution.class))).thenReturn(savedExecution);
        when(responseValidator.validateResponse(anyString(), eq(validationCriteria))).thenReturn(validationResult);

        // Act
        PromptExecutionResult result = testingService.testPrompt(testRequest);

        // Assert
        assertNotNull(result);
        assertTrue(result.getValidationPassed());
        assertEquals(validationResult, result.getValidationResult());

        // Verify validation was performed
        verify(responseValidator).validateResponse(anyString(), eq(validationCriteria));
    }

    @Test
    @DisplayName("Should execute batch test sequentially")
    void shouldExecuteBatchTestSequentially() {
        // Arrange
        List<Map<String, Object>> parameterSets = Arrays.asList(
                Collections.singletonMap("parameter", "value1"),
                Collections.singletonMap("parameter", "value2")
        );

        PromptBatchTestRequest batchRequest = PromptBatchTestRequest.builder()
                .versionId(VERSION_ID)
                .providerId(PROVIDER_ID)
                .modelId(MODEL_ID)
                .parameterSets(parameterSets)
                .parallelExecution(false)
                .storeResults(true)
                .build();

        when(versionRepository.findByIdWithParameters(VERSION_ID)).thenReturn(Optional.of(testVersion));
        when(parameterValidator.validateParameters(eq(testVersion), anyMap())).thenReturn(validParameterResult);
        when(providerFactory.getProvider(PROVIDER_ID)).thenReturn(Optional.of(llmProvider));
        when(llmProvider.executePrompt(any(LlmRequest.class))).thenReturn(llmResponse);
        when(executionRepository.save(any(PromptExecution.class))).thenReturn(savedExecution);

        // Act
        List<PromptExecutionResult> results = testingService.batchTestPrompt(batchRequest);

        // Assert
        assertNotNull(results);
        assertEquals(2, results.size());

        // Verify prompt was tested twice (once for each parameter set)
        verify(llmProvider, times(2)).executePrompt(any(LlmRequest.class));
    }

    @Test
    @DisplayName("Should compare responses from two executions")
    void shouldCompareResponsesFromTwoExecutions() {
        // Arrange
        PromptExecution execution1 = PromptExecution.builder()
                .id("exec1")
                .version(testVersion)
                .rawResponse("This is response 1")
                .tokenCount(30)
                .responseTimeMs(500L)
                .cost(BigDecimal.valueOf(0.002))
                .build();

        PromptExecution execution2 = PromptExecution.builder()
                .id("exec2")
                .version(testVersion)
                .rawResponse("This is a different response")
                .tokenCount(35)
                .responseTimeMs(600L)
                .cost(BigDecimal.valueOf(0.0025))
                .build();

        when(executionRepository.findById("exec1")).thenReturn(Optional.of(execution1));
        when(executionRepository.findById("exec2")).thenReturn(Optional.of(execution2));

        // Act
        Map<String, Object> comparison = testingService.compareResponses("exec1", "exec2");

        // Assert
        assertNotNull(comparison);
        assertEquals(30, comparison.get("tokenCount1"));
        assertEquals(35, comparison.get("tokenCount2"));
        assertEquals(-5.0, comparison.get("tokenCountDiff"));
        assertEquals(500L, comparison.get("responseTime1"));
        assertEquals(600L, comparison.get("responseTime2"));
        assertEquals(-100.0, comparison.get("responseTimeDiff"));

        // Should include similarity score
        assertTrue(comparison.containsKey("similarityScore"));
    }

    @Test
    @DisplayName("Should get test history for a version")
    void shouldGetTestHistoryForVersion() {
        // Arrange
        List<PromptExecution> executions = Arrays.asList(
                savedExecution,
                PromptExecution.builder().id("exec2").version(testVersion).build()
        );

        when(versionRepository.existsById(VERSION_ID)).thenReturn(true);
        when(executionRepository.findLatestByVersionId(eq(VERSION_ID), any(PageRequest.class)))
                .thenReturn(executions);

        // Mock the mapper to convert executions to responses with actual objects
        viettel.dac.promptservice.dto.response.PromptExecutionResponse mockResponse = 
            viettel.dac.promptservice.dto.response.PromptExecutionResponse.builder()
                .id(EXECUTION_ID)
                .versionId(VERSION_ID)
                .templateId(TEMPLATE_ID)
                .providerId(PROVIDER_ID)
                .modelId(MODEL_ID)
                .response("Test response")
                .status(ExecutionStatus.SUCCESS)
                .executedAt(LocalDateTime.now())
                .build();
                
        when(mapper.toExecutionResponse(any(PromptExecution.class)))
                .thenReturn(mockResponse);

        // Act
        List<PromptExecutionResult> history = testingService.getTestHistory(VERSION_ID, 10);

        // Assert
        assertNotNull(history);
        assertEquals(2, history.size());
        assertEquals(EXECUTION_ID, history.get(0).getExecutionId());

        // Verify repository call
        verify(executionRepository).findLatestByVersionId(eq(VERSION_ID), any(PageRequest.class));
    }

    @Test
    @DisplayName("Should throw exception when getting history for non-existent version")
    void shouldThrowExceptionWhenVersionNotFoundForHistory() {
        // Arrange
        when(versionRepository.existsById("non-existent")).thenReturn(false);

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> testingService.getTestHistory("non-existent", 10));

        // Verify no execution lookup was made
        verify(executionRepository, never()).findLatestByVersionId(anyString(), any());
    }

    @Test
    @DisplayName("Should validate response successfully")
    void shouldValidateResponseSuccessfully() {
        // Arrange
        String response = "This is a test response";
        Map<String, Object> criteria = new HashMap<>();
        criteria.put("contains", "test");

        ValidationResult expectedResult = ValidationResult.builder()
                .passed(true)
                .score(1.0)
                .build();

        when(responseValidator.validateResponse(response, criteria)).thenReturn(expectedResult);

        // Act
        ValidationResult result = testingService.validateResponse(response, criteria);

        // Assert
        assertNotNull(result);
        assertTrue(result.isPassed());
        assertEquals(1.0, result.getScore());

        // Verify validator was called
        verify(responseValidator).validateResponse(response, criteria);
    }

    @Test
    @DisplayName("Execute prompt with missing required parameters")
    void executePromptWithMissingRequiredParameters() {
        // Arrange
        PromptVersion testVersion = createTestVersion();
        PromptTestRequest request = new PromptTestRequest();
        request.setVersionId(testVersion.getId());
        request.setProviderId("openai");
        request.setModelId("gpt-3.5-turbo");
        request.setParameters(Collections.emptyMap()); // Missing required parameters
        
        // Mock missing parameter validation
        ParameterValidationResult invalidResult = new ParameterValidationResult();
        invalidResult.setValid(false);
        invalidResult.addIssue("parameter", "Required parameter is missing", ParameterValidationResult.ValidationSeverity.ERROR);
        invalidResult.getMissingRequired().add("parameter");
        
        when(versionRepository.findByIdWithParameters(testVersion.getId())).thenReturn(Optional.of(testVersion));
        when(parameterValidator.validateParameters(eq(testVersion), anyMap())).thenReturn(invalidResult);
        when(executionRepository.save(any(PromptExecution.class))).thenAnswer(i -> i.getArgument(0));
        
        // Act
        PromptExecutionResult result = testingService.testPrompt(request);
        
        // Assert
        assertNotNull(result);
        assertEquals(ExecutionStatus.INVALID_PARAMS, result.getStatus());
        assertNotNull(result.getErrorMessage());
        verify(versionRepository).findByIdWithParameters(testVersion.getId());
        verify(parameterValidator).validateParameters(eq(testVersion), anyMap());
    }

    @Test
    @DisplayName("Execute prompt with timeout from LLM service")
    void executePromptWithLlmTimeout() {
        // Arrange
        PromptVersion testVersion = createTestVersion();
        PromptTestRequest request = new PromptTestRequest();
        request.setVersionId(testVersion.getId());
        request.setProviderId("openai");
        request.setModelId("gpt-3.5-turbo");
        request.setParameters(Collections.singletonMap("parameter", "test value"));
        
        // Mock successful parameter validation
        ParameterValidationResult validResult = new ParameterValidationResult();
        validResult.setValid(true);
        validResult.setValidatedValues(Collections.singletonMap("parameter", "test value"));
        
        LlmProvider mockProvider = mock(LlmProvider.class);
        RuntimeException timeoutException = new RuntimeException("LLM service timeout");
        
        when(versionRepository.findByIdWithParameters(testVersion.getId())).thenReturn(Optional.of(testVersion));
        when(parameterValidator.validateParameters(eq(testVersion), anyMap())).thenReturn(validResult);
        when(providerFactory.getProvider("openai")).thenReturn(Optional.of(mockProvider));
        when(mockProvider.executePrompt(any(LlmRequest.class))).thenThrow(timeoutException);
        
        // Act
        PromptExecutionResult result = testingService.testPrompt(request);
        
        // Assert
        assertNotNull(result);
        assertEquals(ExecutionStatus.ERROR, result.getStatus());
        assertFalse(result.getValidationPassed() != null && result.getValidationPassed());
        verify(versionRepository).findByIdWithParameters(testVersion.getId());
        verify(parameterValidator).validateParameters(eq(testVersion), anyMap());
        verify(providerFactory).getProvider("openai");
        verify(mockProvider).executePrompt(any(LlmRequest.class));
    }

    @Test
    @DisplayName("Save test result with large output content")
    void saveTestResultWithLargeOutputContent() {
        // Arrange
        PromptVersion testVersion = createTestVersion();
        PromptTestRequest request = new PromptTestRequest();
        request.setVersionId(testVersion.getId());
        request.setProviderId("openai");
        request.setModelId("gpt-3.5-turbo");
        request.setParameters(Collections.singletonMap("parameter", "test value"));
        request.setStoreResult(true); // Request to store the execution
        
        // Mock successful parameter validation
        ParameterValidationResult validResult = new ParameterValidationResult();
        validResult.setValid(true);
        validResult.setValidatedValues(Collections.singletonMap("parameter", "test value"));
        
        // Create a large response output
        StringBuilder largeOutput = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            largeOutput.append("This is line ").append(i).append(" of test output. ");
        }
        
        LlmProvider mockProvider = mock(LlmProvider.class);
        LlmResponse largeResponse = new LlmResponse();
        largeResponse.setText(largeOutput.toString());
        largeResponse.setTotalTokenCount(5000);
        
        when(versionRepository.findByIdWithParameters(testVersion.getId())).thenReturn(Optional.of(testVersion));
        when(parameterValidator.validateParameters(eq(testVersion), anyMap())).thenReturn(validResult);
        when(providerFactory.getProvider("openai")).thenReturn(Optional.of(mockProvider));
        when(mockProvider.executePrompt(any(LlmRequest.class))).thenReturn(largeResponse);
        when(executionRepository.save(any(PromptExecution.class))).thenAnswer(i -> i.getArgument(0));
        when(securityUtils.getCurrentUserId()).thenReturn(Optional.of("test-user"));
        
        // Act
        PromptExecutionResult result = testingService.testPrompt(request);
        
        // Assert
        assertNotNull(result);
        assertEquals(ExecutionStatus.SUCCESS, result.getStatus());
        assertTrue(result.getValidationPassed() == null || result.getValidationPassed());
        verify(executionRepository).save(any(PromptExecution.class));
        verify(versionRepository).findByIdWithParameters(testVersion.getId());
        verify(parameterValidator).validateParameters(eq(testVersion), anyMap());
        
        // Verify the content in the result
        assertEquals(largeOutput.toString(), result.getResponse());
    }

    @Test
    @DisplayName("Test parameter substitution with complex template")
    void testParameterSubstitutionWithComplexTemplate() {
        // Arrange
        PromptVersion testVersion = createTestVersion();
        testVersion.setContent("Complex template with {{parameter}} and {{another_param}} and some calculation {{calculation}}");
        
        // Add an additional parameter to the test version
        PromptParameter anotherParam = new PromptParameter();
        anotherParam.setId("param-456");
        anotherParam.setName("another_param");
        anotherParam.setDescription("Another parameter");
        anotherParam.setParameterType(ParameterType.STRING);
        anotherParam.setRequired(true);
        anotherParam.setVersion(testVersion);
        
        PromptParameter calculationParam = new PromptParameter();
        calculationParam.setId("param-789");
        calculationParam.setName("calculation");
        calculationParam.setDescription("Calculation parameter");
        calculationParam.setParameterType(ParameterType.NUMBER);
        calculationParam.setRequired(true);
        calculationParam.setVersion(testVersion);
        
        testVersion.getParameters().add(anotherParam);
        testVersion.getParameters().add(calculationParam);
        
        PromptTestRequest request = new PromptTestRequest();
        request.setVersionId(testVersion.getId());
        request.setProviderId("openai");
        request.setModelId("gpt-3.5-turbo");
        
        // Set all required parameters
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("parameter", "test value");
        parameters.put("another_param", "another test value");
        parameters.put("calculation", 42);
        request.setParameters(parameters);
        
        // Mock successful parameter validation
        ParameterValidationResult validResult = new ParameterValidationResult();
        validResult.setValid(true);
        validResult.setValidatedValues(parameters);
        
        LlmProvider mockProvider = mock(LlmProvider.class);
        LlmResponse response = new LlmResponse();
        response.setText("Generated response");
        response.setTotalTokenCount(100);
        
        when(versionRepository.findByIdWithParameters(testVersion.getId())).thenReturn(Optional.of(testVersion));
        when(parameterValidator.validateParameters(eq(testVersion), anyMap())).thenReturn(validResult);
        when(providerFactory.getProvider("openai")).thenReturn(Optional.of(mockProvider));
        when(mockProvider.executePrompt(any(LlmRequest.class))).thenAnswer(inv -> {
            LlmRequest req = inv.getArgument(0);
            // Verify that the parameters were properly substituted
            assertTrue(req.getPrompt().contains("test value"));
            assertTrue(req.getPrompt().contains("another test value"));
            assertTrue(req.getPrompt().contains("42"));
            return response;
        });
        
        // Act
        PromptExecutionResult result = testingService.testPrompt(request);
        
        // Assert
        assertNotNull(result);
        assertEquals(ExecutionStatus.SUCCESS, result.getStatus());
        assertTrue(result.getValidationPassed() == null || result.getValidationPassed());
        verify(versionRepository).findByIdWithParameters(testVersion.getId());
        verify(parameterValidator).validateParameters(eq(testVersion), anyMap());
        verify(mockProvider).executePrompt(any(LlmRequest.class));
    }

    /**
     * Helper method to create a test version
     */
    private PromptVersion createTestVersion() {
        PromptTemplate template = new PromptTemplate();
        template.setId("template-123");
        template.setName("Test Template");
        
        PromptVersion version = new PromptVersion();
        version.setId("version-123");
        version.setTemplate(template);
        version.setVersionNumber("1.0.0");
        version.setContent("Test prompt with {{parameter}}");
        version.setStatus(VersionStatus.PUBLISHED);
        version.setCreatedAt(LocalDateTime.now());
        version.setCreatedBy("test-user");
        
        // Add a parameter
        Set<PromptParameter> parameters = new HashSet<>();
        PromptParameter parameter = new PromptParameter();
        parameter.setId("param-123");
        parameter.setName("parameter");
        parameter.setDescription("Test parameter");
        parameter.setParameterType(ParameterType.STRING);
        parameter.setRequired(true);
        parameter.setVersion(version);
        parameters.add(parameter);
        
        version.setParameters(parameters);
        
        return version;
    }
}