package viettel.dac.promptservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import viettel.dac.promptservice.dto.request.LlmRequest;
import viettel.dac.promptservice.dto.request.PromptBatchTestRequest;
import viettel.dac.promptservice.dto.request.PromptTestRequest;
import viettel.dac.promptservice.dto.response.LlmResponse;
import viettel.dac.promptservice.dto.response.PromptExecutionResponse;
import viettel.dac.promptservice.dto.response.PromptExecutionResult;
import viettel.dac.promptservice.dto.validation.ParameterValidationResult;
import viettel.dac.promptservice.dto.validation.ValidationResult;
import viettel.dac.promptservice.exception.ResourceNotFoundException;
import viettel.dac.promptservice.exception.ValidationException;
import viettel.dac.promptservice.model.entity.PromptExecution;
import viettel.dac.promptservice.model.entity.PromptVersion;
import viettel.dac.promptservice.model.enums.ExecutionStatus;
import viettel.dac.promptservice.repository.jpa.PromptExecutionRepository;
import viettel.dac.promptservice.repository.jpa.PromptVersionRepository;
import viettel.dac.promptservice.security.SecurityUtils;
import viettel.dac.promptservice.service.llm.LlmProvider;
import viettel.dac.promptservice.service.llm.LlmProviderFactory;
import viettel.dac.promptservice.service.llm.LlmProviderProperties;
import viettel.dac.promptservice.service.mapper.EntityDtoMapper;
import viettel.dac.promptservice.service.preview.PromptTestingService;
import viettel.dac.promptservice.service.validation.ParameterValidator;
import viettel.dac.promptservice.service.validation.ResponseValidator;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Implementation of the PromptTestingService for testing prompts against LLM providers
 * with parameter validation and response validation capabilities
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PromptTestingServiceImpl implements PromptTestingService {

    private final PromptVersionRepository versionRepository;
    private final PromptExecutionRepository executionRepository;
    private final LlmProviderFactory providerFactory;
    private final LlmProviderProperties providerProperties;
    private final ParameterValidator parameterValidator;
    private final ResponseValidator responseValidator;
    private final SecurityUtils securityUtils;
    private final EntityDtoMapper mapper;

    /**
     * Test a prompt against a specified LLM provider
     */
    @Override
    @Transactional
    public PromptExecutionResult testPrompt(PromptTestRequest request) {
        log.debug("Testing prompt version {} with provider {}, model {}",
                request.getVersionId(), request.getProviderId(), request.getModelId());

        // Get prompt version
        PromptVersion version = versionRepository.findByIdWithParameters(request.getVersionId())
                .orElseThrow(() -> new ResourceNotFoundException("Prompt version not found with id: " + request.getVersionId()));

        // Validate parameters
        ParameterValidationResult paramValidation = parameterValidator.validateParameters(
                version, request.getParameters());

        if (!paramValidation.isValid()) {
            return createErrorResult(version, request, paramValidation,
                    "Parameter validation failed", ExecutionStatus.INVALID_PARAMS);
        }

        // Get validated parameters
        Map<String, Object> validatedParams = paramValidation.getValidatedValues();

        try {
            // Get the provider
            LlmProvider provider = providerFactory.getProvider(request.getProviderId())
                    .orElseThrow(() -> new ValidationException("Provider not found: " + request.getProviderId()));

            // Apply parameters to the prompt template
            String promptText = version.applyParameters(validatedParams);

            // Build the LLM request
            LlmRequest llmRequest = buildLlmRequest(request, version, promptText);

            // Execute the prompt against the provider
            LlmResponse llmResponse = provider.executePrompt(llmRequest);

            // Store execution if requested
            PromptExecution execution = null;
            if (request.isStoreResult()) {
                execution = createAndSaveExecution(version, request, llmResponse, validatedParams);
            }

            // Validate response if criteria provided
            ValidationResult validationResult = null;
            boolean validationPassed = true;
            if (request.getValidationCriteria() != null && !request.getValidationCriteria().isEmpty()) {
                validationResult = responseValidator.validateResponse(
                        llmResponse.getText(), request.getValidationCriteria());
                validationPassed = validationResult.isPassed();

                // Update execution with validation results if stored
                if (execution != null) {
                    String validationSummary = "Validation " + (validationPassed ? "passed" : "failed");
                    if (!validationPassed && validationResult.getIssues() != null && !validationResult.getIssues().isEmpty()) {
                        validationSummary += ": " + validationResult.getIssues().get(0).getMessage();
                    }
                    updateExecutionWithValidation(execution.getId(), validationPassed, validationSummary);
                }
            }

            // Build the result DTO
            return buildExecutionResult(version, request, llmResponse, execution,
                    validationResult, validationPassed);

        } catch (Exception e) {
            log.error("Error testing prompt: {}", e.getMessage(), e);
            return createErrorResult(version, request, paramValidation,
                    "Error testing prompt: " + e.getMessage(), ExecutionStatus.ERROR);
        }
    }

    /**
     * Test a prompt asynchronously
     */
    @Override
    public CompletableFuture<PromptExecutionResult> testPromptAsync(PromptTestRequest request) {
        return CompletableFuture.supplyAsync(() -> testPrompt(request));
    }

    /**
     * Execute batch testing of a prompt with multiple parameter sets
     */
    @Override
    public List<PromptExecutionResult> batchTestPrompt(PromptBatchTestRequest request) {
        log.debug("Batch testing prompt version {} with {} parameter sets",
                request.getVersionId(), request.getParameterSets().size());

        // Get prompt version
        PromptVersion version = versionRepository.findByIdWithParameters(request.getVersionId())
                .orElseThrow(() -> new ResourceNotFoundException("Prompt version not found with id: " + request.getVersionId()));

        List<PromptExecutionResult> results = new ArrayList<>();

        if (request.isParallelExecution()) {
            // Execute tests in parallel
            ExecutorService executorService = Executors.newFixedThreadPool(
                    Math.min(request.getMaxConcurrent(), request.getParameterSets().size()));

            try {
                List<CompletableFuture<PromptExecutionResult>> futures = request.getParameterSets().stream()
                        .map(params -> CompletableFuture.supplyAsync(() -> {
                            PromptTestRequest testRequest = PromptTestRequest.builder()
                                    .versionId(request.getVersionId())
                                    .providerId(request.getProviderId())
                                    .modelId(request.getModelId())
                                    .parameters(params)
                                    .maxTokens(request.getMaxTokens())
                                    .temperature(request.getTemperature())
                                    .validationCriteria(request.getValidationCriteria())
                                    .storeResult(request.isStoreResults())
                                    .build();
                            return testPrompt(testRequest);
                        }, executorService))
                        .collect(Collectors.toList());

                // Wait for all futures to complete
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

                // Collect results
                for (CompletableFuture<PromptExecutionResult> future : futures) {
                    results.add(future.join());
                }
            } finally {
                executorService.shutdown();
            }
        } else {
            // Execute tests sequentially
            for (Map<String, Object> params : request.getParameterSets()) {
                PromptTestRequest testRequest = PromptTestRequest.builder()
                        .versionId(request.getVersionId())
                        .providerId(request.getProviderId())
                        .modelId(request.getModelId())
                        .parameters(params)
                        .maxTokens(request.getMaxTokens())
                        .temperature(request.getTemperature())
                        .validationCriteria(request.getValidationCriteria())
                        .storeResult(request.isStoreResults())
                        .build();
                results.add(testPrompt(testRequest));
            }
        }

        return results;
    }

    /**
     * Execute batch testing asynchronously
     */
    @Override
    public CompletableFuture<List<PromptExecutionResult>> batchTestPromptAsync(PromptBatchTestRequest request) {
        return CompletableFuture.supplyAsync(() -> batchTestPrompt(request));
    }

    /**
     * Validate a prompt response against specified validation criteria
     */
    @Override
    public ValidationResult validateResponse(String response, Map<String, Object> validationCriteria) {
        return responseValidator.validateResponse(response, validationCriteria);
    }

    /**
     * Compare responses from two different executions
     */
    @Override
    public Map<String, Object> compareResponses(String executionId1, String executionId2) {
        // Get executions
        PromptExecution execution1 = executionRepository.findById(executionId1)
                .orElseThrow(() -> new ResourceNotFoundException("Execution not found with id: " + executionId1));

        PromptExecution execution2 = executionRepository.findById(executionId2)
                .orElseThrow(() -> new ResourceNotFoundException("Execution not found with id: " + executionId2));

        // Compare metrics
        Map<String, Object> comparison = new HashMap<>();

        // Response metrics comparison
        comparison.put("tokenCount1", execution1.getTokenCount());
        comparison.put("tokenCount2", execution2.getTokenCount());
        comparison.put("tokenCountDiff", calculateDifference(execution1.getTokenCount(), execution2.getTokenCount()));

        comparison.put("responseTime1", execution1.getResponseTimeMs());
        comparison.put("responseTime2", execution2.getResponseTimeMs());
        comparison.put("responseTimeDiff", calculateDifference(execution1.getResponseTimeMs(), execution2.getResponseTimeMs()));

        comparison.put("cost1", execution1.getCost());
        comparison.put("cost2", execution2.getCost());
        comparison.put("costDiff", execution1.getCost() != null && execution2.getCost() != null ?
                execution1.getCost().subtract(execution2.getCost()) : null);

        // Response content comparison
        String response1 = execution1.getRawResponse();
        String response2 = execution2.getRawResponse();

        comparison.put("responseLengthDiff", calculateDifference(
                response1 != null ? response1.length() : 0,
                response2 != null ? response2.length() : 0));

        // Text similarity score (simple Jaccard similarity of words)
        if (response1 != null && response2 != null) {
            comparison.put("similarityScore", calculateJaccardSimilarity(response1, response2));
        }

        return comparison;
    }

    /**
     * Get test history for a prompt version
     */
    @Override
    public List<PromptExecutionResult> getTestHistory(String versionId, int limit) {
        // Check if version exists
        if (!versionRepository.existsById(versionId)) {
            throw new ResourceNotFoundException("Prompt version not found with id: " + versionId);
        }

        // Get execution history
        List<PromptExecution> executions = executionRepository
                .findLatestByVersionId(versionId, PageRequest.of(0, limit));

        // Convert to DTOs
        return executions.stream()
                .map(e -> mapper.toExecutionResponse(e))
                .map(this::convertToExecutionResult)
                .collect(Collectors.toList());
    }

    //-------------------- Helper Methods --------------------//

    /**
     * Build the LLM request from the test request and prompt text
     */
    private LlmRequest buildLlmRequest(PromptTestRequest request, PromptVersion version, String promptText) {
        return LlmRequest.builder()
                .providerId(request.getProviderId())
                .modelId(request.getModelId())
                .prompt(promptText)
                .systemPrompt(version.getSystemPrompt())
                .maxTokens(request.getMaxTokens() != null ? request.getMaxTokens() : providerProperties.getDefaultMaxTokens())
                .temperature(request.getTemperature() != null ? request.getTemperature() : providerProperties.getDefaultTemperature())
                .timeoutMs(providerProperties.getDefaultTimeoutMs())
                .build();
    }

    /**
     * Create and save an execution record in the database
     */
    private PromptExecution createAndSaveExecution(PromptVersion version, PromptTestRequest request,
                                                   LlmResponse llmResponse, Map<String, Object> validatedParams) {

        String currentUser = securityUtils.getCurrentUserId().orElse("system");

        PromptExecution execution = PromptExecution.builder()
                .version(version)
                .providerId(request.getProviderId())
                .modelId(request.getModelId())
                .inputParameters(validatedParams)
                .rawResponse(llmResponse.getText())
                .tokenCount(llmResponse.getTotalTokenCount())
                .inputTokens(llmResponse.getInputTokenCount())
                .outputTokens(llmResponse.getOutputTokenCount())
                .cost(llmResponse.getCost() != null ?
                        java.math.BigDecimal.valueOf(llmResponse.getCost()) : null)
                .responseTimeMs(llmResponse.getResponseTimeMs())
                .executedAt(LocalDateTime.now())
                .executedBy(currentUser)
                .status(ExecutionStatus.SUCCESS)
                .build();

        return executionRepository.save(execution);
    }

    /**
     * Update an execution record with validation results
     */
    private void updateExecutionWithValidation(String executionId, boolean passed, String validationSummary) {
        executionRepository.findById(executionId).ifPresent(execution -> {
            String updatedResponse = execution.getRawResponse();
            if (updatedResponse != null) {
                updatedResponse += "\n\n--- Validation: " + (passed ? "PASSED" : "FAILED") + " ---\n";
                updatedResponse += validationSummary;
                execution.setRawResponse(updatedResponse);
                executionRepository.save(execution);
            }
        });
    }

    /**
     * Build the execution result DTO from the response and execution
     */
    private PromptExecutionResult buildExecutionResult(PromptVersion version, PromptTestRequest request,
                                                       LlmResponse llmResponse, PromptExecution execution, ValidationResult validationResult,
                                                       boolean validationPassed) {

        return PromptExecutionResult.builder()
                .executionId(execution != null ? execution.getId() : null)
                .versionId(version.getId())
                .templateId(version.getTemplate().getId())
                .providerId(request.getProviderId())
                .modelId(request.getModelId())
                .parameters(request.getParameters())
                .response(llmResponse.getText())
                .tokenCount(llmResponse.getTotalTokenCount())
                .inputTokens(llmResponse.getInputTokenCount())
                .outputTokens(llmResponse.getOutputTokenCount())
                .cost(llmResponse.getCost() != null ?
                        java.math.BigDecimal.valueOf(llmResponse.getCost()) : null)
                .responseTimeMs(llmResponse.getResponseTimeMs())
                .executedAt(LocalDateTime.now())
                .executedBy(securityUtils.getCurrentUserId().orElse("system"))
                .status(ExecutionStatus.SUCCESS)
                .validationResult(validationResult)
                .validationPassed(validationPassed)
                .metadata(llmResponse.getMetadata())
                .build();
    }

    /**
     * Create an error result for failed tests
     */
    private PromptExecutionResult createErrorResult(PromptVersion version, PromptTestRequest request,
                                                    ParameterValidationResult paramValidation, String errorMessage, ExecutionStatus status) {

        // Create execution record in the database if requested
        PromptExecution execution = null;
        if (request.isStoreResult()) {
            execution = PromptExecution.builder()
                    .version(version)
                    .providerId(request.getProviderId())
                    .modelId(request.getModelId())
                    .inputParameters(request.getParameters())
                    .rawResponse("Error: " + errorMessage)
                    .executedAt(LocalDateTime.now())
                    .executedBy(securityUtils.getCurrentUserId().orElse("system"))
                    .status(status)
                    .build();

            execution = executionRepository.save(execution);
        }

        // Build error result
        return PromptExecutionResult.builder()
                .executionId(execution != null ? execution.getId() : null)
                .versionId(version.getId())
                .templateId(version.getTemplate().getId())
                .providerId(request.getProviderId())
                .modelId(request.getModelId())
                .parameters(request.getParameters())
                .response("Error: " + errorMessage)
                .executedAt(LocalDateTime.now())
                .executedBy(securityUtils.getCurrentUserId().orElse("system"))
                .status(status)
                .errorMessage(errorMessage)
                .build();
    }

    /**
     * Calculate difference between two numbers
     */
    private double calculateDifference(Number n1, Number n2) {
        if (n1 == null || n2 == null) {
            return 0;
        }
        return n1.doubleValue() - n2.doubleValue();
    }

    /**
     * Calculate Jaccard similarity between two texts
     */
    private double calculateJaccardSimilarity(String text1, String text2) {
        // Simple word-based Jaccard similarity
        Set<String> words1 = tokenizeToWords(text1);
        Set<String> words2 = tokenizeToWords(text2);

        // Calculate intersection size
        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);

        // Calculate union size
        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);

        // Avoid division by zero
        if (union.isEmpty()) {
            return 1.0; // Both texts are empty, consider them identical
        }

        return (double) intersection.size() / union.size();
    }

    /**
     * Tokenize text to words for similarity calculation
     */
    private Set<String> tokenizeToWords(String text) {
        return Arrays.stream(text.toLowerCase()
                        .replaceAll("[^a-z0-9\\s]", " ")
                        .split("\\s+"))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    /**
     * Convert execution response to execution result
     */
    private PromptExecutionResult convertToExecutionResult(PromptExecutionResponse response) {
        return PromptExecutionResult.builder()
                .executionId(response.getId())
                .versionId(response.getVersionId())
                .templateId(response.getTemplateId())
                .providerId(response.getProviderId())
                .modelId(response.getModelId())
                .parameters(response.getInputParameters())
                .response(response.getResponse())
                .tokenCount(response.getTokenCount())
                .inputTokens(response.getInputTokens())
                .outputTokens(response.getOutputTokens())
                .cost(response.getCost())
                .responseTimeMs(response.getResponseTimeMs())
                .executedAt(response.getExecutedAt())
                .executedBy(response.getExecutedBy())
                .status(response.getStatus())
                .build();
    }
}