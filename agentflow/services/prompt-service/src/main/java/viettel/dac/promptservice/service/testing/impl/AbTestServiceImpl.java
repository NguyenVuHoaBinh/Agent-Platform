package viettel.dac.promptservice.service.testing.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.stat.inference.AlternativeHypothesis;
import org.apache.commons.math3.stat.inference.BinomialTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import viettel.dac.promptservice.dto.request.AbTestRequest;
import viettel.dac.promptservice.dto.request.AbTestUpdateRequest;
import viettel.dac.promptservice.dto.request.PromptTestRequest;
import viettel.dac.promptservice.dto.response.AbTestResponse;
import viettel.dac.promptservice.dto.response.PromptExecutionResult;
import viettel.dac.promptservice.exception.ResourceNotFoundException;
import viettel.dac.promptservice.exception.ValidationException;
import viettel.dac.promptservice.model.entity.AbTest;
import viettel.dac.promptservice.model.entity.AbTestResult;
import viettel.dac.promptservice.model.entity.PromptVersion;
import viettel.dac.promptservice.model.enums.TestStatus;
import viettel.dac.promptservice.model.enums.VersionStatus;
import viettel.dac.promptservice.repository.jpa.AbTestRepository;
import viettel.dac.promptservice.repository.jpa.AbTestResultRepository;
import viettel.dac.promptservice.repository.jpa.PromptVersionRepository;
import viettel.dac.promptservice.security.SecurityUtils;
import viettel.dac.promptservice.service.impl.PromptVersionServiceImpl;
import viettel.dac.promptservice.service.impl.PromptTestingServiceImpl;
import viettel.dac.promptservice.service.testing.AbTestService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Implementation of A/B testing service
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AbTestServiceImpl implements AbTestService {

    private final AbTestRepository testRepository;
    private final AbTestResultRepository resultRepository;
    private final PromptVersionRepository versionRepository;
    private final PromptTestingServiceImpl testingService;
    private final PromptVersionServiceImpl versionService;
    private final SecurityUtils securityUtils;

    // Default metrics
    private static final String METRIC_SUCCESS_RATE = "success_rate";
    private static final String METRIC_RESPONSE_TIME = "response_time";
    private static final String METRIC_TOKEN_USAGE = "token_usage";
    private static final String METRIC_COST = "cost";

    // Default batch size for test iterations
    private static final int DEFAULT_BATCH_SIZE = 10;

    @Override
    @Transactional
    public AbTestResponse createTest(AbTestRequest request) {
        log.debug("Creating new A/B test: {}", request.getName());

        // Validate request
        validateTestRequest(request);

        // Get the versions
        PromptVersion controlVersion = versionRepository.findById(request.getControlVersionId())
                .orElseThrow(() -> new ResourceNotFoundException("Control version not found with id: " + request.getControlVersionId()));

        PromptVersion variantVersion = versionRepository.findById(request.getVariantVersionId())
                .orElseThrow(() -> new ResourceNotFoundException("Variant version not found with id: " + request.getVariantVersionId()));

        // Validate versions are from the same template
        if (!controlVersion.getTemplate().getId().equals(variantVersion.getTemplate().getId())) {
            throw new ValidationException("Control and variant versions must be from the same template");
        }

        // Create test entity
        AbTest test = AbTest.builder()
                .name(request.getName())
                .description(request.getDescription())
                .status(TestStatus.CREATED)
                .createdBy(securityUtils.getCurrentUserId().orElse("system"))
                .controlVersion(controlVersion)
                .variantVersion(variantVersion)
                .sampleSize(request.getSampleSize())
                .confidenceThreshold(request.getConfidenceThreshold())
                .evaluationMetric(request.getEvaluationMetric())
                .testParameters(request.getTestParameters())
                .successCriteria(request.getSuccessCriteria())
                .providerId(request.getProviderId())
                .modelId(request.getModelId())
                .build();

        // Save test
        AbTest savedTest = testRepository.save(test);

        // Create initial results
        AbTestResult controlResult = AbTestResult.builder()
                .test(savedTest)
                .version(controlVersion)
                .controlVersion(true)
                .sampleCount(0)
                .successCount(0)
                .successRate(0.0)
                .metricValues(new HashMap<>())
                .executionIds(new HashMap<>())
                .build();

        AbTestResult variantResult = AbTestResult.builder()
                .test(savedTest)
                .version(variantVersion)
                .controlVersion(false)
                .sampleCount(0)
                .successCount(0)
                .successRate(0.0)
                .metricValues(new HashMap<>())
                .executionIds(new HashMap<>())
                .build();

        savedTest.addResult(controlResult);
        savedTest.addResult(variantResult);

        // Save updated test with results
        savedTest = testRepository.save(savedTest);

        // Start the test if requested
        if (request.isStartImmediately()) {
            savedTest.setStatus(TestStatus.RUNNING);
            savedTest.setStartedAt(LocalDateTime.now());
            savedTest = testRepository.save(savedTest);

            // Run first test iteration asynchronously
            runTestAsync(savedTest.getId());
        }

        return convertToResponse(savedTest);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AbTestResponse> getTestById(String testId) {
        log.debug("Getting A/B test with ID: {}", testId);

        return testRepository.findByIdWithResults(testId)
                .map(this::convertToResponse);
    }

    @Override
    @Transactional
    public AbTestResponse updateTest(String testId, AbTestUpdateRequest request) {
        log.debug("Updating A/B test with ID: {}", testId);

        // Get test
        AbTest test = testRepository.findByIdWithResults(testId)
                .orElseThrow(() -> new ResourceNotFoundException("Test not found with id: " + testId));

        // Validate update is allowed
        if (test.isComplete()) {
            throw new ValidationException("Cannot update completed test");
        }

        // Update fields
        if (request.getName() != null) {
            test.setName(request.getName());
        }

        if (request.getDescription() != null) {
            test.setDescription(request.getDescription());
        }

        if (request.getSampleSize() != null) {
            test.setSampleSize(request.getSampleSize());
        }

        if (request.getConfidenceThreshold() != null) {
            test.setConfidenceThreshold(request.getConfidenceThreshold());
        }

        if (request.getEvaluationMetric() != null) {
            test.setEvaluationMetric(request.getEvaluationMetric());
        }

        if (request.getTestParameters() != null) {
            test.setTestParameters(request.getTestParameters());
        }

        if (request.getSuccessCriteria() != null) {
            test.setSuccessCriteria(request.getSuccessCriteria());
        }

        if (request.getProviderId() != null) {
            test.setProviderId(request.getProviderId());
        }

        if (request.getModelId() != null) {
            test.setModelId(request.getModelId());
        }

        // Handle status changes
        if (request.getStatus() != null && test.getStatus() != request.getStatus()) {
            switch (request.getStatus()) {
                case RUNNING:
                    if (test.getStatus() == TestStatus.CREATED || test.getStatus() == TestStatus.PAUSED) {
                        test.setStatus(TestStatus.RUNNING);
                        if (test.getStartedAt() == null) {
                            test.setStartedAt(LocalDateTime.now());
                        }
                    } else {
                        throw new ValidationException("Cannot start test from status: " + test.getStatus());
                    }
                    break;
                case PAUSED:
                    if (test.getStatus() == TestStatus.RUNNING) {
                        test.setStatus(TestStatus.PAUSED);
                    } else {
                        throw new ValidationException("Cannot pause test from status: " + test.getStatus());
                    }
                    break;
                case COMPLETED:
                    if (test.getStatus() == TestStatus.RUNNING || test.getStatus() == TestStatus.PAUSED) {
                        test.setStatus(TestStatus.COMPLETED);
                        test.setCompletedAt(LocalDateTime.now());
                    } else {
                        throw new ValidationException("Cannot complete test from status: " + test.getStatus());
                    }
                    break;
                case CANCELLED:
                    if (test.getStatus() != TestStatus.COMPLETED) {
                        test.setStatus(TestStatus.CANCELLED);
                        test.setCompletedAt(LocalDateTime.now());
                    } else {
                        throw new ValidationException("Cannot cancel completed test");
                    }
                    break;
                default:
                    throw new ValidationException("Invalid status change requested");
            }
        }

        // Save updated test
        AbTest updatedTest = testRepository.save(test);

        // Run test if started
        if (updatedTest.getStatus() == TestStatus.RUNNING) {
            runTestAsync(updatedTest.getId());
        }

        return convertToResponse(updatedTest);
    }

    @Override
    @Transactional
    public void deleteTest(String testId) {
        log.debug("Deleting A/B test with ID: {}", testId);

        // Check if test exists
        if (!testRepository.existsById(testId)) {
            throw new ResourceNotFoundException("Test not found with id: " + testId);
        }

        // Delete test
        testRepository.deleteById(testId);
    }

    @Override
    @Transactional
    public AbTestResponse startTest(String testId) {
        log.debug("Starting A/B test with ID: {}", testId);

        // Get test
        AbTest test = testRepository.findById(testId)
                .orElseThrow(() -> new ResourceNotFoundException("Test not found with id: " + testId));

        // Validate test can be started
        if (!test.isReadyToStart() && test.getStatus() != TestStatus.PAUSED) {
            throw new ValidationException("Test cannot be started from status: " + test.getStatus());
        }

        // Update status
        test.setStatus(TestStatus.RUNNING);
        if (test.getStartedAt() == null) {
            test.setStartedAt(LocalDateTime.now());
        }

        // Save updated test
        AbTest updatedTest = testRepository.save(test);

        // Run test asynchronously
        runTestAsync(updatedTest.getId());

        return convertToResponse(updatedTest);
    }

    @Override
    @Transactional
    public AbTestResponse pauseTest(String testId) {
        log.debug("Pausing A/B test with ID: {}", testId);

        // Get test
        AbTest test = testRepository.findById(testId)
                .orElseThrow(() -> new ResourceNotFoundException("Test not found with id: " + testId));

        // Validate test can be paused
        if (!test.isActive()) {
            throw new ValidationException("Test cannot be paused from status: " + test.getStatus());
        }

        // Update status
        test.setStatus(TestStatus.PAUSED);

        // Save updated test
        AbTest updatedTest = testRepository.save(test);

        return convertToResponse(updatedTest);
    }

    @Override
    @Transactional
    public AbTestResponse resumeTest(String testId) {
        log.debug("Resuming A/B test with ID: {}", testId);

        // Get test
        AbTest test = testRepository.findById(testId)
                .orElseThrow(() -> new ResourceNotFoundException("Test not found with id: " + testId));

        // Validate test can be resumed
        if (test.getStatus() != TestStatus.PAUSED) {
            throw new ValidationException("Test cannot be resumed from status: " + test.getStatus());
        }

        // Update status
        test.setStatus(TestStatus.RUNNING);

        // Save updated test
        AbTest updatedTest = testRepository.save(test);

        // Run test asynchronously
        runTestAsync(updatedTest.getId());

        return convertToResponse(updatedTest);
    }

    @Override
    @Transactional
    public AbTestResponse completeTest(String testId) {
        log.debug("Completing A/B test with ID: {}", testId);

        // Get test
        AbTest test = testRepository.findById(testId)
                .orElseThrow(() -> new ResourceNotFoundException("Test not found with id: " + testId));

        // Validate test can be completed
        if (test.isComplete()) {
            throw new ValidationException("Test is already complete");
        }

        // Update status
        test.setStatus(TestStatus.COMPLETED);
        test.setCompletedAt(LocalDateTime.now());

        // Save updated test
        AbTest updatedTest = testRepository.save(test);

        // Calculate final statistics
        return calculateTestStatistics(updatedTest.getId());
    }

    @Override
    @Transactional
    public AbTestResponse cancelTest(String testId) {
        log.debug("Cancelling A/B test with ID: {}", testId);

        // Get test
        AbTest test = testRepository.findById(testId)
                .orElseThrow(() -> new ResourceNotFoundException("Test not found with id: " + testId));

        // Validate test can be cancelled
        if (test.isComplete()) {
            throw new ValidationException("Test is already complete");
        }

        // Update status
        test.setStatus(TestStatus.CANCELLED);
        test.setCompletedAt(LocalDateTime.now());

        // Save updated test
        AbTest updatedTest = testRepository.save(test);

        return convertToResponse(updatedTest);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AbTestResponse> getTestsByStatus(TestStatus status, Pageable pageable) {
        log.debug("Getting A/B tests with status: {}", status);

        // Handle null status by returning all tests
        if (status == null) {
            return testRepository.findAll(pageable)
                    .map(this::convertToResponse);
        }

        return testRepository.findByStatus(status, pageable)
                .map(this::convertToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AbTestResponse> getTestsByCreator(String createdBy, Pageable pageable) {
        log.debug("Getting A/B tests by creator: {}", createdBy);

        return testRepository.findByCreatedBy(createdBy, pageable)
                .map(this::convertToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AbTestResponse> searchTestsByName(String name, Pageable pageable) {
        log.debug("Searching A/B tests by name: {}", name);

        return testRepository.findByNameContainingIgnoreCase(name, pageable)
                .map(this::convertToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AbTestResponse> getTestsByTemplateId(String templateId, Pageable pageable) {
        log.debug("Getting A/B tests by template ID: {}", templateId);

        return testRepository.findByTemplateId(templateId, pageable)
                .map(this::convertToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AbTestResponse> getTestsByVersionId(String versionId, Pageable pageable) {
        log.debug("Getting A/B tests by version ID: {}", versionId);

        return testRepository.findByVersionId(versionId, pageable)
                .map(this::convertToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AbTestResponse> getTestsByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        log.debug("Getting A/B tests by date range from {} to {}", startDate, endDate);

        return testRepository.findByDateRange(startDate, endDate, pageable)
                .map(this::convertToResponse);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public AbTest runTestIteration(AbTest test) {
        log.debug("Running iteration for A/B test with ID: {}", test.getId());

        // Check if test is active
        if (!test.isActive()) {
            log.debug("Test is not active, skipping iteration");
            return test;
        }

        // Get results
        List<AbTestResult> results = test.getResults();
        if (results.size() != 2) {
            log.error("Expected 2 results for test {}, found {}", test.getId(), results.size());
            throw new ValidationException("Invalid test configuration: requires exactly 2 results");
        }

        // Get control and variant results
        AbTestResult controlResult = results.stream()
                .filter(r -> r.isControlVersion())
                .findFirst()
                .orElseThrow(() -> new ValidationException("Control result not found"));

        AbTestResult variantResult = results.stream()
                .filter(r -> !r.isControlVersion())
                .findFirst()
                .orElseThrow(() -> new ValidationException("Variant result not found"));

        // Determine batch size
        int controlBatchSize = test.getSampleSize() - controlResult.getSampleCount();
        int variantBatchSize = test.getSampleSize() - variantResult.getSampleCount();

        int batchSize = Math.min(
                Math.min(controlBatchSize, variantBatchSize),
                DEFAULT_BATCH_SIZE
        );

        // If both versions have reached sample size, complete the test
        if (controlBatchSize <= 0 && variantBatchSize <= 0) {
            log.debug("Test has reached target sample size, marking as complete");
            test.setStatus(TestStatus.COMPLETED);
            test.setCompletedAt(LocalDateTime.now());
            return testRepository.save(test);
        }

        // Run control batch if needed
        if (controlBatchSize > 0 && batchSize > 0) {
            runTestBatch(test, controlResult, batchSize);
        }

        // Run variant batch if needed
        if (variantBatchSize > 0 && batchSize > 0) {
            runTestBatch(test, variantResult, batchSize);
        }

        // Calculate statistics
        calculateStatistics(test);

        // Check if statistical significance reached
        boolean significanceReached = checkStatisticalSignificance(test);

        // If significance reached and auto-complete enabled, complete the test
        if (significanceReached) {
            log.debug("Statistical significance reached, marking test as complete");
            test.setStatus(TestStatus.COMPLETED);
            test.setCompletedAt(LocalDateTime.now());
        }

        // Save updated test
        return testRepository.save(test);
    }

    /**
     * Run a batch of tests for a specific version
     */
    private void runTestBatch(AbTest test, AbTestResult result, int batchSize) {
        Map<String, Object> parameters = test.getTestParameters() != null ?
                test.getTestParameters() : new HashMap<>();

        // Create a test request
        PromptTestRequest testRequest = PromptTestRequest.builder()
                .versionId(result.getVersion().getId())
                .providerId(test.getProviderId())
                .modelId(test.getModelId())
                .parameters(parameters)
                .storeResult(true)
                .build();

        // Add success criteria if defined
        if (test.getSuccessCriteria() != null && !test.getSuccessCriteria().isEmpty()) {
            testRequest.setValidationCriteria(Map.of("custom_criteria", test.getSuccessCriteria()));
        }

        // Run the tests
        int successes = 0;
        double totalResponseTime = 0;
        long totalTokens = 0;
        BigDecimal totalCost = BigDecimal.ZERO;
        List<String> executionIds = new ArrayList<>();

        for (int i = 0; i < batchSize; i++) {
            try {
                PromptExecutionResult executionResult = testingService.testPrompt(testRequest);

                // Track execution ID
                if (executionResult.getExecutionId() != null) {
                    executionIds.add(executionResult.getExecutionId());
                }

                // Determine success based on evaluation metric
                boolean success = evaluateSuccess(test.getEvaluationMetric(), executionResult);
                if (success) {
                    successes++;
                }

                // Track metrics
                if (executionResult.getResponseTimeMs() != null) {
                    totalResponseTime += executionResult.getResponseTimeMs();
                }

                if (executionResult.getTokenCount() != null) {
                    totalTokens += executionResult.getTokenCount();
                }

                if (executionResult.getCost() != null) {
                    totalCost = totalCost.add(executionResult.getCost());
                }

            } catch (Exception e) {
                log.error("Error running test execution for A/B test {}: {}", test.getId(), e.getMessage());
            }
        }

        // Add execution IDs to result
        Map<String, Object> executionIdsMap = result.getExecutionIds() != null ?
                result.getExecutionIds() : new HashMap<>();
        String batchKey = "batch_" + (result.getSampleCount() / batchSize + 1);
        executionIdsMap.put(batchKey, executionIds);
        result.setExecutionIds(executionIdsMap);

        // Update metrics
        double avgResponseTime = batchSize > 0 ? totalResponseTime / batchSize : 0;
        double avgTokens = batchSize > 0 ? (double) totalTokens / batchSize : 0;
        BigDecimal avgCost = batchSize > 0 ?
                totalCost.divide(BigDecimal.valueOf(batchSize), 6, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        // Update metric values
        Map<String, Object> metricValues = result.getMetricValues() != null ?
                result.getMetricValues() : new HashMap<>();

        metricValues.put("avg_response_time", avgResponseTime);
        metricValues.put("avg_tokens", avgTokens);
        metricValues.put("avg_cost", avgCost);
        result.setMetricValues(metricValues);

        // Update result fields
        result.updateCounts(batchSize, successes);

        double overallAvgResponseTime = result.getAverageResponseTime() != null ?
                result.getAverageResponseTime() : 0;
        double newAvgResponseTime = (overallAvgResponseTime * (result.getSampleCount() - batchSize) +
                totalResponseTime) / result.getSampleCount();
        result.setAverageResponseTime(newAvgResponseTime);

        double overallAvgTokens = result.getAverageTokens() != null ?
                result.getAverageTokens() : 0;
        double newAvgTokens = (overallAvgTokens * (result.getSampleCount() - batchSize) +
                totalTokens) / result.getSampleCount();
        result.setAverageTokens(newAvgTokens);

        BigDecimal overallAvgCost = result.getAverageCost() != null ?
                result.getAverageCost() : BigDecimal.ZERO;
        BigDecimal previousCost = overallAvgCost.multiply(BigDecimal.valueOf(result.getSampleCount() - batchSize));
        BigDecimal newAvgCost = previousCost.add(totalCost)
                .divide(BigDecimal.valueOf(result.getSampleCount()), 6, RoundingMode.HALF_UP);
        result.setAverageCost(newAvgCost);

        BigDecimal newTotalCost = result.getTotalCost() != null ?
                result.getTotalCost().add(totalCost) : totalCost;
        result.setTotalCost(newTotalCost);
    }

    /**
     * Evaluate success based on evaluation metric
     */
    private boolean evaluateSuccess(String evaluationMetric, PromptExecutionResult executionResult) {
        // Handle built-in metrics
        switch (evaluationMetric) {
            case METRIC_SUCCESS_RATE:
                // Success based on execution status
                return executionResult.getStatus() != null &&
                        executionResult.getStatus().name().equals("SUCCESS");

            case METRIC_RESPONSE_TIME:
                // Low response time is better (below median)
                // This is a placeholder, would need historical data to determine median
                return executionResult.getResponseTimeMs() != null &&
                        executionResult.getResponseTimeMs() < 1000; // Example threshold

            case METRIC_TOKEN_USAGE:
                // Low token usage is better (below median)
                // This is a placeholder, would need historical data to determine median
                return executionResult.getTokenCount() != null &&
                        executionResult.getTokenCount() < 100; // Example threshold

            case METRIC_COST:
                // Low cost is better (below median)
                // This is a placeholder, would need historical data to determine median
                return executionResult.getCost() != null &&
                        executionResult.getCost().compareTo(BigDecimal.valueOf(0.01)) < 0; // Example threshold

            default:
                // Custom validation criteria
                return executionResult.getValidationPassed() != null &&
                        executionResult.getValidationPassed();
        }
    }

    /**
     * Calculate statistics for a test
     */
    private void calculateStatistics(AbTest test) {
        List<AbTestResult> results = test.getResults();
        if (results.size() != 2) {
            return;
        }

        AbTestResult controlResult = results.stream()
                .filter(r -> r.isControlVersion())
                .findFirst()
                .orElse(null);

        AbTestResult variantResult = results.stream()
                .filter(r -> !r.isControlVersion())
                .findFirst()
                .orElse(null);

        if (controlResult == null || variantResult == null) {
            return;
        }

        // Calculate p-value using binomial test
        if (controlResult.getSampleCount() > 0 && variantResult.getSampleCount() > 0) {
            BinomialTest binomialTest = new BinomialTest();

            try {
                // Control success rate as the expected probability
                double controlSuccessRate = controlResult.getSuccessRate() / 100.0;
                // Prevent division by zero or extreme values
                if (controlSuccessRate <= 0) controlSuccessRate = 0.01;
                if (controlSuccessRate >= 1) controlSuccessRate = 0.99;

                // Calculate p-value for variant
                double pValue = binomialTest.binomialTest(
                        variantResult.getSampleCount(),
                        variantResult.getSuccessCount(),
                        controlSuccessRate,
                        AlternativeHypothesis.TWO_SIDED);

                variantResult.setPValue(pValue);
                variantResult.setConfidenceLevel((1 - pValue) * 100);

                // Also update control
                controlResult.setPValue(0.5); // For control, p-value is 0.5 by definition
                controlResult.setConfidenceLevel(50.0); // 50% confidence

            } catch (Exception e) {
                log.error("Error calculating statistics for test {}: {}", test.getId(), e.getMessage());
            }
        }
    }

    /**
     * Check if statistical significance has been reached
     */
    private boolean checkStatisticalSignificance(AbTest test) {
        List<AbTestResult> results = test.getResults();
        if (results.size() != 2) {
            return false;
        }

        AbTestResult controlResult = results.stream()
                .filter(AbTestResult::isControlVersion)
                .findFirst()
                .orElse(null);

        AbTestResult variantResult = results.stream()
                .filter(r -> !r.isControlVersion())
                .findFirst()
                .orElse(null);

        if (controlResult == null || variantResult == null ||
                controlResult.getPValue() == null || variantResult.getPValue() == null) {
            return false;
        }

        // Check if minimum sample size reached
        if (controlResult.getSampleCount() < 30 || variantResult.getSampleCount() < 30) {
            return false;
        }

        // Check if variant is significantly better
        return variantResult.isSignificantlyBetter(controlResult);
    }

    @Override
    @Async("taskExecutor")
    public CompletableFuture<AbTestResponse> runTestAsync(String testId) {
        log.debug("Running A/B test asynchronously with ID: {}", testId);

        try {
            // Get test
            AbTest test = testRepository.findByIdWithResults(testId)
                    .orElseThrow(() -> new ResourceNotFoundException("Test not found with id: " + testId));

            // Check if test is active
            if (!test.isActive()) {
                log.debug("Test is not active, skipping execution");
                return CompletableFuture.completedFuture(convertToResponse(test));
            }

            // Run the test iteration
            AbTest updatedTest = runTestIteration(test);

            // If test is still active, schedule next iteration
            if (updatedTest.isActive()) {
                CompletableFuture.runAsync(() -> {
                    try {
                        // Wait a bit to avoid hitting rate limits
                        Thread.sleep(1000);
                        runTestAsync(updatedTest.getId());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }

            return CompletableFuture.completedFuture(convertToResponse(updatedTest));

        } catch (Exception e) {
            log.error("Error running A/B test {}: {}", testId, e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    @Transactional
    public String applyTestWinner(String testId) {
        log.debug("Applying A/B test winner for test with ID: {}", testId);

        // Get test
        AbTest test = testRepository.findByIdWithResults(testId)
                .orElseThrow(() -> new ResourceNotFoundException("Test not found with id: " + testId));

        // Validate test is complete
        if (!test.isComplete()) {
            throw new ValidationException("Cannot apply winner for incomplete test");
        }

        // Find the winner
        List<AbTestResult> results = test.getResults();

        AbTestResult controlResult = results.stream()
                .filter(r -> r.isControlVersion())
                .findFirst()
                .orElseThrow(() -> new ValidationException("Control result not found"));

        AbTestResult variantResult = results.stream()
                .filter(r -> !r.isControlVersion())
                .findFirst()
                .orElseThrow(() -> new ValidationException("Variant result not found"));

        // Determine which version is better
        String winnerId;

        if (variantResult.isSignificantlyBetter(controlResult)) {
            // Variant wins
            winnerId = variantResult.getVersion().getId();
        } else {
            // Control wins or no significant difference
            winnerId = controlResult.getVersion().getId();
        }

        // Publish the winning version
        versionService.updateVersionStatus(winnerId, VersionStatus.PUBLISHED);

        return winnerId;
    }

    @Override
    @Transactional
    public AbTestResponse calculateTestStatistics(String testId) {
        log.debug("Calculating statistics for A/B test with ID: {}", testId);

        // Get test
        AbTest test = testRepository.findByIdWithResults(testId)
                .orElseThrow(() -> new ResourceNotFoundException("Test not found with id: " + testId));

        // Calculate statistics
        calculateStatistics(test);

        // Save updated test
        AbTest updatedTest = testRepository.save(test);

        return convertToResponse(updatedTest);
    }

    /**
     * Convert test entity to response DTO
     */
    private AbTestResponse convertToResponse(AbTest test) {
        if (test == null) {
            return null;
        }

        // Get results, ensuring they're loaded
        List<AbTestResult> results = test.getResults();
        if (results == null) {
            results = resultRepository.findByTestId(test.getId());
        }

        // Build version infos
        AbTestResponse.VersionInfo controlVersionInfo = AbTestResponse.VersionInfo.builder()
                .id(test.getControlVersion().getId())
                .versionNumber(test.getControlVersion().getVersionNumber())
                .templateId(test.getControlVersion().getTemplate().getId())
                .templateName(test.getControlVersion().getTemplate().getName())
                .build();

        AbTestResponse.VersionInfo variantVersionInfo = AbTestResponse.VersionInfo.builder()
                .id(test.getVariantVersion().getId())
                .versionNumber(test.getVariantVersion().getVersionNumber())
                .templateId(test.getVariantVersion().getTemplate().getId())
                .templateName(test.getVariantVersion().getTemplate().getName())
                .build();

        // Build result infos
        List<AbTestResponse.ResultInfo> resultInfos = results.stream()
                .map(result -> AbTestResponse.ResultInfo.builder()
                        .id(result.getId())
                        .versionId(result.getVersion().getId())
                        .isControlVersion(result.isControlVersion())
                        .sampleCount(result.getSampleCount())
                        .successCount(result.getSuccessCount())
                        .successRate(result.getSuccessRate())
                        .averageResponseTime(result.getAverageResponseTime())
                        .averageTokens(result.getAverageTokens())
                        .averageCost(result.getAverageCost() != null ? result.getAverageCost().doubleValue() : null)
                        .totalCost(result.getTotalCost() != null ? result.getTotalCost().doubleValue() : null)
                        .pValue(result.getPValue())
                        .confidenceLevel(result.getConfidenceLevel())
                        .metricValues(result.getMetricValues())
                        .build())
                .collect(Collectors.toList());

        // Calculate overall progress
        int targetSamples = test.getSampleSize() * 2; // Both versions
        int actualSamples = results.stream()
                .mapToInt(AbTestResult::getSampleCount)
                .sum();
        double progress = targetSamples > 0 ? (double) actualSamples / targetSamples * 100 : 0;

        // Build test outcome if test is complete
        AbTestResponse.TestOutcome outcome = null;
        if (test.isComplete() && results.size() == 2) {
            AbTestResult controlResult = results.stream()
                    .filter(AbTestResult::isControlVersion)
                    .findFirst()
                    .orElse(null);

            AbTestResult variantResult = results.stream()
                    .filter(r -> !r.isControlVersion())
                    .findFirst()
                    .orElse(null);

            if (controlResult != null && variantResult != null) {
                // Determine winner
                String winnerId;
                String winnerName;
                double improvementPercentage = 0;
                boolean significantDifference = false;
                Double confidenceLevel = null;
                String recommendation;

                if (variantResult.isSignificantlyBetter(controlResult)) {
                    // Variant wins
                    winnerId = variantResult.getVersion().getId();
                    winnerName = "Variant (" + test.getVariantVersion().getVersionNumber() + ")";
                    improvementPercentage = (variantResult.getSuccessRate() - controlResult.getSuccessRate())
                            / controlResult.getSuccessRate() * 100;
                    significantDifference = true;
                    confidenceLevel = variantResult.getConfidenceLevel();
                    recommendation = "Apply variant version";
                } else if (controlResult.getSuccessRate() > variantResult.getSuccessRate() &&
                        (controlResult.getPValue() != null && controlResult.getPValue() < 0.5)) {
                    // Control wins
                    winnerId = controlResult.getVersion().getId();
                    winnerName = "Control (" + test.getControlVersion().getVersionNumber() + ")";
                    improvementPercentage = 0; // No improvement
                    significantDifference = true;
                    confidenceLevel = controlResult.getConfidenceLevel();
                    recommendation = "Keep control version";
                } else {
                    // No significant difference
                    winnerId = controlResult.getVersion().getId(); // Default to control
                    winnerName = "No clear winner";
                    improvementPercentage = 0;
                    significantDifference = false;
                    confidenceLevel = 50.0; // No confidence
                    recommendation = "More testing needed or keep control version";
                }

                outcome = AbTestResponse.TestOutcome.builder()
                        .winnerId(winnerId)
                        .winnerName(winnerName)
                        .improvementPercentage(Math.round(improvementPercentage * 100) / 100.0)
                        .significantDifference(significantDifference)
                        .confidenceLevel(confidenceLevel)
                        .recommendation(recommendation)
                        .build();
            }
        }

        // Build the response
        return AbTestResponse.builder()
                .id(test.getId())
                .name(test.getName())
                .description(test.getDescription())
                .status(test.getStatus())
                .createdBy(test.getCreatedBy())
                .createdAt(test.getCreatedAt())
                .updatedAt(test.getUpdatedAt())
                .startedAt(test.getStartedAt())
                .completedAt(test.getCompletedAt())
                .controlVersion(controlVersionInfo)
                .variantVersion(variantVersionInfo)
                .sampleSize(test.getSampleSize())
                .confidenceThreshold(test.getConfidenceThreshold())
                .evaluationMetric(test.getEvaluationMetric())
                .testParameters(test.getTestParameters())
                .successCriteria(test.getSuccessCriteria())
                .providerId(test.getProviderId())
                .modelId(test.getModelId())
                .progress(Math.min(100, Math.round(progress * 100) / 100.0))
                .results(resultInfos)
                .outcome(outcome)
                .build();
    }

    /**
     * Validate test request
     */
    private void validateTestRequest(AbTestRequest request) {
        if (request.getControlVersionId().equals(request.getVariantVersionId())) {
            throw new ValidationException("Control and variant versions must be different");
        }

        if (request.getSampleSize() <= 0) {
            throw new ValidationException("Sample size must be positive");
        }

        if (request.getConfidenceThreshold() < 0 || request.getConfidenceThreshold() > 100) {
            throw new ValidationException("Confidence threshold must be between 0 and 100");
        }

        // Validate metric is supported
        if (!isValidMetric(request.getEvaluationMetric())) {
            throw new ValidationException("Unsupported evaluation metric: " + request.getEvaluationMetric());
        }

        // Validate provider and model if specified
        if (request.getProviderId() == null || request.getProviderId().isEmpty()) {
            throw new ValidationException("Provider ID is required");
        }

        if (request.getModelId() == null || request.getModelId().isEmpty()) {
            throw new ValidationException("Model ID is required");
        }
    }

    /**
     * Check if metric is valid
     */
    private boolean isValidMetric(String metric) {
        return metric != null && (
                metric.equals(METRIC_SUCCESS_RATE) ||
                        metric.equals(METRIC_RESPONSE_TIME) ||
                        metric.equals(METRIC_TOKEN_USAGE) ||
                        metric.equals(METRIC_COST) ||
                        metric.startsWith("custom_")
        );
    }
}