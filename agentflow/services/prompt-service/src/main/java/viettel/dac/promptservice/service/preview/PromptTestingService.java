package viettel.dac.promptservice.service.preview;

import viettel.dac.promptservice.dto.request.PromptBatchTestRequest;
import viettel.dac.promptservice.dto.request.PromptTestRequest;
import viettel.dac.promptservice.dto.response.PromptExecutionResult;
import viettel.dac.promptservice.dto.validation.ValidationResult;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Service for testing prompts against LLM providers with validation capabilities
 */
public interface PromptTestingService {

    /**
     * Test a prompt against a specified LLM provider
     *
     * @param request The test request containing version, provider, and parameters
     * @return The execution result with response data and metrics
     */
    PromptExecutionResult testPrompt(PromptTestRequest request);

    /**
     * Asynchronously test a prompt
     *
     * @param request The test request
     * @return CompletableFuture with the execution result
     */
    CompletableFuture<PromptExecutionResult> testPromptAsync(PromptTestRequest request);

    /**
     * Execute batch testing of a prompt with multiple parameter sets
     *
     * @param request The batch test request
     * @return List of execution results for each parameter set
     */
    List<PromptExecutionResult> batchTestPrompt(PromptBatchTestRequest request);

    /**
     * Execute batch testing asynchronously
     *
     * @param request The batch test request
     * @return CompletableFuture with list of execution results
     */
    CompletableFuture<List<PromptExecutionResult>> batchTestPromptAsync(PromptBatchTestRequest request);

    /**
     * Validate a prompt response against specified validation criteria
     *
     * @param response The LLM response text
     * @param validationCriteria Map of validation rules to apply
     * @return Validation result with details on pass/fail and issues
     */
    ValidationResult validateResponse(String response, Map<String, Object> validationCriteria);

    /**
     * Compare responses from two different executions
     *
     * @param executionId1 First execution ID
     * @param executionId2 Second execution ID
     * @return Comparison metrics and analysis
     */
    Map<String, Object> compareResponses(String executionId1, String executionId2);

    /**
     * Get test history for a prompt version
     *
     * @param versionId The prompt version ID
     * @param limit Maximum number of records to return
     * @return List of past execution results
     */
    List<PromptExecutionResult> getTestHistory(String versionId, int limit);
}
