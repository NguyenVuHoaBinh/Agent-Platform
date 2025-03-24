package viettel.dac.promptservice.service.batch.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import viettel.dac.promptservice.dto.optimization.OptimizationResult;
import viettel.dac.promptservice.dto.optimization.PromptOptimizationRequest;
import viettel.dac.promptservice.dto.optimization.SuggestionType;
import viettel.dac.promptservice.dto.request.BatchJobRequest;
import viettel.dac.promptservice.dto.request.BatchJobUpdateRequest;
import viettel.dac.promptservice.dto.request.PromptTestRequest;
import viettel.dac.promptservice.dto.response.BatchJobResponse;
import viettel.dac.promptservice.dto.response.PromptExecutionResult;
import viettel.dac.promptservice.exception.ResourceNotFoundException;
import viettel.dac.promptservice.exception.ValidationException;
import viettel.dac.promptservice.model.entity.BatchJob;
import viettel.dac.promptservice.model.entity.BatchJobExecution;
import viettel.dac.promptservice.model.entity.PromptVersion;
import viettel.dac.promptservice.model.enums.BatchJobStatus;
import viettel.dac.promptservice.model.enums.BatchJobType;
import viettel.dac.promptservice.repository.jpa.BatchJobExecutionRepository;
import viettel.dac.promptservice.repository.jpa.BatchJobRepository;
import viettel.dac.promptservice.repository.jpa.PromptTemplateRepository;
import viettel.dac.promptservice.repository.jpa.PromptVersionRepository;
import viettel.dac.promptservice.security.SecurityUtils;
import viettel.dac.promptservice.service.batch.BatchJobService;
import viettel.dac.promptservice.service.impl.PromptTestingServiceImpl;
import viettel.dac.promptservice.service.optimization.PromptOptimizationService;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Implementation of batch job service
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BatchJobServiceImpl implements BatchJobService {

    private final BatchJobRepository jobRepository;
    private final BatchJobExecutionRepository executionRepository;
    private final PromptVersionRepository versionRepository;
    private final PromptTemplateRepository templateRepository;
    private final SecurityUtils securityUtils;
    private final PromptTestingServiceImpl testingService;
    private final PromptOptimizationService optimizationService;

    // Constants
    private static final int BATCH_SIZE = 10;
    private static final long DEFAULT_JOB_TIMEOUT_HOURS = 24;
    private static final int MAX_CONCURRENT_JOBS = 5;
    private static final String WORKER_ID = UUID.randomUUID().toString();

    @Override
    @Transactional
    public String createJob(BatchJobRequest request) {
        log.debug("Creating batch job: {}", request.getName());

        // Validate request
        validateJobRequest(request);

        // Get user ID
        String currentUserId = securityUtils.getCurrentUserId()
                .orElse("system");

        // Resolve template and version
        PromptVersion version = null;
        if (request.getVersionId() != null) {
            version = versionRepository.findById(request.getVersionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Version not found with id: " + request.getVersionId()));
        }

        // Create job entity
        BatchJob job = BatchJob.builder()
                .name(request.getName())
                .description(request.getDescription())
                .jobType(request.getJobType())
                .status(BatchJobStatus.PENDING)
                .createdBy(currentUserId)
                .scheduledAt(request.getScheduledAt())
                .priority(request.getPriority())
                .maxRetries(request.getMaxRetries())
                .retryCount(0)
                .completionPercentage(0)
                .version(version)
                .template(version != null ? version.getTemplate() : null)
                .parameters(request.getParameters())
                .configuration(request.getConfiguration())
                .result(new HashMap<>())
                .build();

        // Save job
        BatchJob savedJob = jobRepository.save(job);

        // Start job immediately if requested
        if (request.isStartImmediately()) {
            savedJob.setStatus(BatchJobStatus.RUNNING);
            savedJob.setStartedAt(LocalDateTime.now());
            savedJob = jobRepository.save(savedJob);

            // Process job asynchronously
            processJobAsync(savedJob.getId());
        }

        return savedJob.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BatchJobResponse> getJobById(String jobId) {
        log.debug("Getting batch job with ID: {}", jobId);

        return jobRepository.findByIdWithExecutions(jobId)
                .map(this::convertToResponse);
    }

    @Override
    @Transactional
    public BatchJobResponse updateJob(String jobId, BatchJobUpdateRequest request) {
        log.debug("Updating batch job with ID: {}", jobId);

        BatchJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + jobId));

        // Validate update
        if (job.isCompleted()) {
            throw new ValidationException("Cannot update completed job");
        }

        // Update fields
        if (request.getName() != null) {
            job.setName(request.getName());
        }

        if (request.getDescription() != null) {
            job.setDescription(request.getDescription());
        }

        if (request.getScheduledAt() != null) {
            job.setScheduledAt(request.getScheduledAt());
        }

        if (request.getPriority() != null) {
            job.setPriority(request.getPriority());
        }

        if (request.getMaxRetries() != null) {
            job.setMaxRetries(request.getMaxRetries());
        }

        if (request.getParameters() != null) {
            job.setParameters(request.getParameters());
        }

        if (request.getConfiguration() != null) {
            job.setConfiguration(request.getConfiguration());
        }

        // Handle status changes
        if (request.getStatus() != null && job.getStatus() != request.getStatus()) {
            switch (request.getStatus()) {
                case RUNNING:
                    if (job.getStatus() == BatchJobStatus.PENDING || job.getStatus() == BatchJobStatus.SCHEDULED) {
                        job.setStatus(BatchJobStatus.RUNNING);
                        job.setStartedAt(LocalDateTime.now());

                        // Process job asynchronously
                        processJobAsync(job.getId());
                    } else {
                        throw new ValidationException("Cannot start job from status: " + job.getStatus());
                    }
                    break;

                case CANCELLED:
                    if (job.getStatus() != BatchJobStatus.COMPLETED && job.getStatus() != BatchJobStatus.FAILED) {
                        job.setStatus(BatchJobStatus.CANCELLED);
                        job.setCompletedAt(LocalDateTime.now());
                    } else {
                        throw new ValidationException("Cannot cancel completed or failed job");
                    }
                    break;

                default:
                    throw new ValidationException("Cannot manually set job status to: " + request.getStatus());
            }
        }

        // Save updated job
        BatchJob updatedJob = jobRepository.save(job);

        return convertToResponse(updatedJob);
    }

    @Override
    @Transactional
    public void deleteJob(String jobId) {
        log.debug("Deleting batch job with ID: {}", jobId);

        if (!jobRepository.existsById(jobId)) {
            throw new ResourceNotFoundException("Job not found with id: " + jobId);
        }

        jobRepository.deleteById(jobId);
    }

    @Override
    @Transactional
    public BatchJobResponse startJob(String jobId) {
        log.debug("Starting batch job with ID: {}", jobId);

        BatchJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + jobId));

        // Validate start
        if (job.isRunning() || job.isCompleted()) {
            throw new ValidationException("Cannot start job with status: " + job.getStatus());
        }

        // Update status
        job.setStatus(BatchJobStatus.RUNNING);
        job.setStartedAt(LocalDateTime.now());

        // Save updated job
        BatchJob updatedJob = jobRepository.save(job);

        // Process job asynchronously
        processJobAsync(updatedJob.getId());

        return convertToResponse(updatedJob);
    }

    @Override
    @Transactional
    public BatchJobResponse cancelJob(String jobId) {
        log.debug("Cancelling batch job with ID: {}", jobId);

        BatchJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + jobId));

        // Validate cancel
        if (job.isCompleted()) {
            throw new ValidationException("Cannot cancel completed job");
        }

        // Update status
        job.setStatus(BatchJobStatus.CANCELLED);
        job.setCompletedAt(LocalDateTime.now());

        // Save updated job
        BatchJob updatedJob = jobRepository.save(job);

        return convertToResponse(updatedJob);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BatchJobResponse> getJobsByStatus(BatchJobStatus status, Pageable pageable) {
        log.debug("Getting batch jobs with status: {}", status);

        return jobRepository.findByStatus(status, pageable)
                .map(this::convertToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BatchJobResponse> getJobsByType(BatchJobType jobType, Pageable pageable) {
        log.debug("Getting batch jobs with type: {}", jobType);

        return jobRepository.findByJobType(jobType, pageable)
                .map(this::convertToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BatchJobResponse> getJobsByCreator(String createdBy, Pageable pageable) {
        log.debug("Getting batch jobs by creator: {}", createdBy);

        return jobRepository.findByCreatedBy(createdBy, pageable)
                .map(this::convertToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BatchJobResponse> searchJobsByName(String name, Pageable pageable) {
        log.debug("Searching batch jobs by name: {}", name);

        return jobRepository.findByNameContainingIgnoreCase(name, pageable)
                .map(this::convertToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BatchJobExecution> getJobExecutions(String jobId, Pageable pageable) {
        log.debug("Getting executions for batch job with ID: {}", jobId);

        if (!jobRepository.existsById(jobId)) {
            throw new ResourceNotFoundException("Job not found with id: " + jobId);
        }

        return executionRepository.findByJobId(jobId, pageable).getContent();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public BatchJob processJob(BatchJob job) {
        log.debug("Processing batch job: {} ({})", job.getName(), job.getId());

        // Check if job should be processed
        if (!job.isRunning()) {
            log.debug("Job is not in running state, skipping processing");
            return job;
        }

        // Create a new execution
        BatchJobExecution execution = BatchJobExecution.builder()
                .job(job)
                .status(BatchJobStatus.RUNNING)
                .workerId(WORKER_ID)
                .executionParameters(job.getParameters())
                .build();

        execution.start();
        job.addExecution(execution);

        try {
            // Process based on job type
            switch (job.getJobType()) {
                case PROMPT_OPTIMIZATION:
                    processOptimizationJob(job);
                    break;

                case BATCH_EXECUTION:
                    processBatchExecutionJob(job);
                    break;

                case PROMPT_VARIATION:
                    processVariationJob(job);
                    break;

                case DATA_IMPORT:
                case DATA_EXPORT:
                case PERFORMANCE_ANALYSIS:
                case CUSTOM:
                    processCustomJob(job);
                    break;

                default:
                    throw new ValidationException("Unsupported job type: " + job.getJobType());
            }

            // Complete the execution
            execution.complete(job.getResult());

        } catch (Exception e) {
            log.error("Error processing job {}: {}", job.getId(), e.getMessage(), e);

            // Mark execution as failed
            execution.fail(e.getMessage());

            // Mark job as failed or retry
            handleJobFailure(job, e.getMessage());
        }

        // Save execution
        executionRepository.save(execution);

        return job;
    }

    /**
     * Handle job failure
     */
    private void handleJobFailure(BatchJob job, String errorMessage) {
        job.setErrorMessage(errorMessage);

        if (job.getRetryCount() < job.getMaxRetries()) {
            // Retry
            job.setRetryCount(job.getRetryCount() + 1);
            job.setStatus(BatchJobStatus.PENDING);
            job.setScheduledAt(LocalDateTime.now().plusMinutes(5 * job.getRetryCount())); // Exponential backoff
        } else {
            // Max retries exceeded
            job.setStatus(BatchJobStatus.FAILED);
            job.setCompletedAt(LocalDateTime.now());
        }
    }

    @Override
    @Async("taskExecutor")
    public CompletableFuture<BatchJobResponse> processJobAsync(String jobId) {
        log.debug("Processing batch job asynchronously with ID: {}", jobId);

        try {
            // Get job
            BatchJob job = jobRepository.findByIdWithExecutions(jobId)
                    .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + jobId));

            // Process job
            BatchJob processedJob = processJob(job);

            // Save result
            BatchJob savedJob = jobRepository.save(processedJob);

            return CompletableFuture.completedFuture(convertToResponse(savedJob));

        } catch (Exception e) {
            log.error("Error processing job {}: {}", jobId, e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    @Transactional
    public BatchJobResponse scheduleJob(String jobId, LocalDateTime scheduledTime) {
        log.debug("Scheduling batch job {} to run at: {}", jobId, scheduledTime);

        BatchJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + jobId));

        // Validate scheduling
        if (job.isRunning() || job.isCompleted()) {
            throw new ValidationException("Cannot schedule job with status: " + job.getStatus());
        }

        if (scheduledTime.isBefore(LocalDateTime.now())) {
            throw new ValidationException("Scheduled time must be in the future");
        }

        // Update job
        job.setStatus(BatchJobStatus.SCHEDULED);
        job.setScheduledAt(scheduledTime);

        // Save updated job
        BatchJob updatedJob = jobRepository.save(job);

        return convertToResponse(updatedJob);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getJobStatistics() {
        log.debug("Getting batch job statistics");

        Map<String, Object> statistics = new HashMap<>();

        // Count jobs by status
        statistics.put("pendingJobs", jobRepository.countByStatus(BatchJobStatus.PENDING));
        statistics.put("scheduledJobs", jobRepository.countByStatus(BatchJobStatus.SCHEDULED));
        statistics.put("runningJobs", jobRepository.countByStatus(BatchJobStatus.RUNNING));
        statistics.put("completedJobs", jobRepository.countByStatus(BatchJobStatus.COMPLETED));
        statistics.put("failedJobs", jobRepository.countByStatus(BatchJobStatus.FAILED));
        statistics.put("cancelledJobs", jobRepository.countByStatus(BatchJobStatus.CANCELLED));

        // Count jobs by type
        statistics.put("optimizationJobs", jobRepository.countByJobType(BatchJobType.PROMPT_OPTIMIZATION));
        statistics.put("batchExecutionJobs", jobRepository.countByJobType(BatchJobType.BATCH_EXECUTION));
        statistics.put("variationJobs", jobRepository.countByJobType(BatchJobType.PROMPT_VARIATION));

        // Average duration by job type
        List<Object[]> avgDurationByType = executionRepository.getAverageDurationByJobType();
        Map<String, Double> durationByType = new HashMap<>();

        for (Object[] row : avgDurationByType) {
            BatchJobType type = (BatchJobType) row[0];
            Double avgDuration = (Double) row[1];
            durationByType.put(type.name(), avgDuration);
        }

        statistics.put("averageDurationByType", durationByType);

        return statistics;
    }

    @Override
    @Transactional
    public BatchJobResponse retryJob(String jobId) {
        log.debug("Retrying failed batch job with ID: {}", jobId);

        BatchJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + jobId));

        // Validate retry
        if (job.getStatus() != BatchJobStatus.FAILED) {
            throw new ValidationException("Can only retry failed jobs");
        }

        // Update job
        job.setStatus(BatchJobStatus.PENDING);
        job.setRetryCount(job.getRetryCount() + 1);
        job.setErrorMessage(null);

        // Save updated job
        BatchJob updatedJob = jobRepository.save(job);

        return convertToResponse(updatedJob);
    }

    @Override
    public BatchJob processOptimizationJob(BatchJob job) {
        log.debug("Processing optimization job: {}", job.getId());

        // Validate job
        if (job.getVersion() == null) {
            throw new ValidationException("Optimization job requires a version");
        }

        // Get optimization request from parameters
        PromptOptimizationRequest request = getOptimizationRequest(job.getParameters());

        // Create execution log
        StringBuilder executionLog = new StringBuilder();
        executionLog.append("Starting optimization for version: ").append(job.getVersion().getVersionNumber()).append("\n");

        // Get latest execution
        BatchJobExecution execution = job.getLatestExecution();
        if (execution != null) {
            execution.appendToLog(executionLog.toString());
        }

        // Progress updates
        job.updateProgress(10);
        appendToLog(execution, "Analyzing prompt...");

        // Start with analysis
        OptimizationResult analysis = optimizationService.analyzePrompt(job.getVersion().getId());

        job.updateProgress(40);
        appendToLog(execution, "Analysis complete. Found " + analysis.getSuggestions().size() + " suggestions.");

        // Filter suggestions by requested types
        List<OptimizationResult.Suggestion> filteredSuggestions = analysis.getSuggestions().stream()
                .filter(s -> request.getSuggestionTypes().contains(s.getType()))
                .toList();

        analysis.setSuggestions(filteredSuggestions);

        // Apply optimizations if requested
        if (request.isApplyAutomatically() && !filteredSuggestions.isEmpty()) {
            appendToLog(execution, "Applying optimizations...");

            // Apply strategies
            String optimizedText = optimizationService.applyOptimizationStrategies(
                    job.getVersion().getId(), request.getStrategies());

            job.updateProgress(70);
            appendToLog(execution, "Optimizations applied.");

            // Create new version if requested
            if (request.isCreateNewVersion()) {
                appendToLog(execution, "Creating new optimized version...");
                PromptVersion optimizedVersion = optimizationService.createOptimizedVersion(
                        job.getVersion().getId(), optimizedText);

                job.getResult().put("optimizedVersionId", optimizedVersion.getId());
                job.getResult().put("optimizedVersionNumber", optimizedVersion.getVersionNumber());

                appendToLog(execution, "Created optimized version: " + optimizedVersion.getVersionNumber());
            }

            job.getResult().put("optimizedText", optimizedText);
        }

        // Store analysis results
        job.getResult().put("analysisScore", analysis.getScore());
        job.getResult().put("suggestionCount", analysis.getSuggestions().size());
        job.getResult().put("suggestions", convertSuggestionsToMap(analysis.getSuggestions()));

        job.updateProgress(100);
        appendToLog(execution, "Optimization job completed successfully.");

        // Set job as completed
        job.setStatus(BatchJobStatus.COMPLETED);
        job.setCompletedAt(LocalDateTime.now());

        return job;
    }

    /**
     * Convert suggestions to a simplified map format
     */
    private List<Map<String, Object>> convertSuggestionsToMap(List<OptimizationResult.Suggestion> suggestions) {
        return suggestions.stream()
                .map(s -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("type", s.getType().name());
                    map.put("description", s.getDescription());
                    map.put("severity", s.getSeverity().name());
                    map.put("explanation", s.getExplanation());
                    map.put("originalText", s.getOriginalText());
                    map.put("suggestedText", s.getSuggestedText());
                    return map;
                })
                .collect(Collectors.toList());
    }

    @Override
    public BatchJob processBatchExecutionJob(BatchJob job) {
        log.debug("Processing batch execution job: {}", job.getId());

        // Validate job
        if (job.getVersion() == null) {
            throw new ValidationException("Batch execution job requires a version");
        }

        // Get job parameters
        Map<String, Object> parameters = job.getParameters();
        if (parameters == null) {
            parameters = new HashMap<>();
        }

        String providerId = (String) parameters.getOrDefault("providerId", "openai");
        String modelId = (String) parameters.getOrDefault("modelId", "gpt-4");
        int batchSize = (int) parameters.getOrDefault("batchSize", BATCH_SIZE);
        int totalCount = (int) parameters.getOrDefault("totalCount", 100);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> parameterSets = (List<Map<String, Object>>) parameters.get("parameterSets");

        // Create execution log
        StringBuilder executionLog = new StringBuilder();
        executionLog.append("Starting batch execution for version: ").append(job.getVersion().getVersionNumber()).append("\n");
        executionLog.append("Total executions: ").append(parameterSets != null ? parameterSets.size() : totalCount).append("\n");

        // Get latest execution
        BatchJobExecution execution = job.getLatestExecution();
        if (execution != null) {
            execution.appendToLog(executionLog.toString());
        }

        // Initialize results
        List<Map<String, Object>> results = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;
        int totalTokens = 0;
        double totalCost = 0;
        double totalResponseTime = 0;

        // Process in batches
        int processedCount = 0;

        if (parameterSets != null && !parameterSets.isEmpty()) {
            // Use provided parameter sets
            for (int i = 0; i < parameterSets.size(); i += batchSize) {
                if (job.getStatus() != BatchJobStatus.RUNNING) {
                    // Job was cancelled or failed
                    break;
                }

                int endIndex = Math.min(i + batchSize, parameterSets.size());
                List<Map<String, Object>> batch = parameterSets.subList(i, endIndex);

                appendToLog(execution, "Processing batch " + (i / batchSize + 1) + " of " +
                        ((parameterSets.size() + batchSize - 1) / batchSize) +
                        " (" + i + "-" + (endIndex - 1) + ")");

                // Process batch
                for (Map<String, Object> params : batch) {
                    PromptTestRequest testRequest = PromptTestRequest.builder()
                            .versionId(job.getVersion().getId())
                            .providerId(providerId)
                            .modelId(modelId)
                            .parameters(params)
                            .storeResult(true)
                            .build();

                    try {
                        PromptExecutionResult result = testingService.testPrompt(testRequest);

                        // Track metrics
                        if (result.getStatus() != null && result.getStatus().name().equals("SUCCESS")) {
                            successCount++;
                        } else {
                            failureCount++;
                        }

                        if (result.getTokenCount() != null) {
                            totalTokens += result.getTokenCount();
                        }

                        if (result.getCost() != null) {
                            totalCost += result.getCost().doubleValue();
                        }

                        if (result.getResponseTimeMs() != null) {
                            totalResponseTime += result.getResponseTimeMs();
                        }

                        // Store result
                        Map<String, Object> resultMap = new HashMap<>();
                        resultMap.put("executionId", result.getExecutionId());
                        resultMap.put("status", result.getStatus().name());
                        resultMap.put("parameters", result.getParameters());
                        resultMap.put("tokenCount", result.getTokenCount());
                        resultMap.put("responseTimeMs", result.getResponseTimeMs());
                        resultMap.put("cost", result.getCost());

                        results.add(resultMap);

                    } catch (Exception e) {
                        log.error("Error executing prompt test: {}", e.getMessage());
                        failureCount++;

                        // Store error
                        Map<String, Object> errorMap = new HashMap<>();
                        errorMap.put("status", "ERROR");
                        errorMap.put("parameters", params);
                        errorMap.put("error", e.getMessage());

                        results.add(errorMap);
                    }

                    processedCount++;
                }

                // Update progress
                int progress = (int) ((processedCount * 100.0) / parameterSets.size());
                job.updateProgress(progress);

                // Avoid rate limiting
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } else {
            // Generate random test cases
            for (int i = 0; i < totalCount; i += batchSize) {
                if (job.getStatus() != BatchJobStatus.RUNNING) {
                    // Job was cancelled or failed
                    break;
                }

                int endIndex = Math.min(i + batchSize, totalCount);
                int batchCount = endIndex - i;

                appendToLog(execution, "Processing batch " + (i / batchSize + 1) + " of " +
                        ((totalCount + batchSize - 1) / batchSize) +
                        " (" + i + "-" + (endIndex - 1) + ")");

                // Generate and process batch
                for (int j = 0; j < batchCount; j++) {
                    Map<String, Object> params = generateRandomParameters(job.getVersion());

                    PromptTestRequest testRequest = PromptTestRequest.builder()
                            .versionId(job.getVersion().getId())
                            .providerId(providerId)
                            .modelId(modelId)
                            .parameters(params)
                            .storeResult(true)
                            .build();

                    try {
                        PromptExecutionResult result = testingService.testPrompt(testRequest);

                        // Track metrics
                        if (result.getStatus() != null && result.getStatus().name().equals("SUCCESS")) {
                            successCount++;
                        } else {
                            failureCount++;
                        }

                        if (result.getTokenCount() != null) {
                            totalTokens += result.getTokenCount();
                        }

                        if (result.getCost() != null) {
                            totalCost += result.getCost().doubleValue();
                        }

                        if (result.getResponseTimeMs() != null) {
                            totalResponseTime += result.getResponseTimeMs();
                        }

                        // Store result
                        Map<String, Object> resultMap = new HashMap<>();
                        resultMap.put("executionId", result.getExecutionId());
                        resultMap.put("status", result.getStatus().name());
                        resultMap.put("parameters", result.getParameters());
                        resultMap.put("tokenCount", result.getTokenCount());
                        resultMap.put("responseTimeMs", result.getResponseTimeMs());
                        resultMap.put("cost", result.getCost());

                        results.add(resultMap);

                    } catch (Exception e) {
                        log.error("Error executing prompt test: {}", e.getMessage());
                        failureCount++;

                        // Store error
                        Map<String, Object> errorMap = new HashMap<>();
                        errorMap.put("status", "ERROR");
                        errorMap.put("parameters", params);
                        errorMap.put("error", e.getMessage());

                        results.add(errorMap);
                    }

                    processedCount++;
                }

                // Update progress
                int progress = (int) ((processedCount * 100.0) / totalCount);
                job.updateProgress(progress);

                // Avoid rate limiting
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        // Compute summary metrics
        double successRate = processedCount > 0 ? (successCount * 100.0) / processedCount : 0;
        double avgTokens = successCount > 0 ? (double) totalTokens / successCount : 0;
        double avgCost = successCount > 0 ? totalCost / successCount : 0;
        double avgResponseTime = successCount > 0 ? totalResponseTime / successCount : 0;

        // Store results
        job.getResult().put("results", results);
        job.getResult().put("totalExecutions", processedCount);
        job.getResult().put("successCount", successCount);
        job.getResult().put("failureCount", failureCount);
        job.getResult().put("successRate", Math.round(successRate * 100) / 100.0);
        job.getResult().put("totalTokens", totalTokens);
        job.getResult().put("totalCost", Math.round(totalCost * 1000) / 1000.0);
        job.getResult().put("avgTokens", Math.round(avgTokens * 100) / 100.0);
        job.getResult().put("avgCost", Math.round(avgCost * 1000) / 1000.0);
        job.getResult().put("avgResponseTime", Math.round(avgResponseTime * 100) / 100.0);

        job.updateProgress(100);
        appendToLog(execution, "Batch execution job completed successfully.");

        // Set job as completed if not already
        if (job.getStatus() == BatchJobStatus.RUNNING) {
            job.setStatus(BatchJobStatus.COMPLETED);
            job.setCompletedAt(LocalDateTime.now());
        }

        return job;
    }

    /**
     * Process prompt variation job
     */
    private BatchJob processVariationJob(BatchJob job) {
        log.debug("Processing variation job: {}", job.getId());

        // Validate job
        if (job.getVersion() == null) {
            throw new ValidationException("Variation job requires a version");
        }

        // Get job parameters
        Map<String, Object> parameters = job.getParameters();
        if (parameters == null) {
            parameters = new HashMap<>();
        }

        int variationCount = (int) parameters.getOrDefault("variationCount", 5);

        @SuppressWarnings("unchecked")
        List<String> suggestionTypeStrings = (List<String>) parameters.getOrDefault(
                "suggestionTypes",
                List.of("TOKEN_EFFICIENCY", "CLARITY", "PARAMETER_USAGE"));

        List<SuggestionType> suggestionTypes = suggestionTypeStrings.stream()
                .map(SuggestionType::valueOf)
                .collect(Collectors.toList());

        // Create execution log
        StringBuilder executionLog = new StringBuilder();
        executionLog.append("Starting prompt variation generation for version: ")
                .append(job.getVersion().getVersionNumber()).append("\n");
        executionLog.append("Variation count: ").append(variationCount).append("\n");

        // Get latest execution
        BatchJobExecution execution = job.getLatestExecution();
        if (execution != null) {
            execution.appendToLog(executionLog.toString());
        }

        job.updateProgress(10);
        appendToLog(execution, "Analyzing prompt and generating variations...");

        // Generate variations
        Map<String, Map<String, Object>> variations = optimizationService.generateOptimizedVariations(
                job.getVersion().getId(), suggestionTypes, variationCount);

        job.updateProgress(70);
        appendToLog(execution, "Generated " + variations.size() + " variations.");

        // Create versions if requested
        Boolean createVersions = (Boolean) parameters.getOrDefault("createVersions", false);
        List<Map<String, Object>> versionDetails = new ArrayList<>();

        if (createVersions && !variations.isEmpty()) {
            appendToLog(execution, "Creating new versions for variations...");

            int count = 0;
            for (Map.Entry<String, Map<String, Object>> entry : variations.entrySet()) {
                String variationText = entry.getKey();
                Map<String, Object> variationMetadata = entry.getValue();

                // Create version
                String versionSuffix = "Variation " + (++count);
                if (variationMetadata.containsKey("optimizationType")) {
                    versionSuffix += " (" + variationMetadata.get("optimizationType") + ")";
                }

                try {
                    PromptVersion variationVersion = optimizationService.createOptimizedVersion(
                            job.getVersion().getId(), variationText);

                    Map<String, Object> versionDetail = new HashMap<>();
                    versionDetail.put("versionId", variationVersion.getId());
                    versionDetail.put("versionNumber", variationVersion.getVersionNumber());
                    versionDetail.put("metadata", variationMetadata);

                    versionDetails.add(versionDetail);

                    appendToLog(execution, "Created variation version: " + variationVersion.getVersionNumber());

                } catch (Exception e) {
                    log.error("Error creating variation version: {}", e.getMessage());
                    appendToLog(execution, "Error creating variation: " + e.getMessage());
                }
            }
        }

        // Store results
        job.getResult().put("variations", variations);
        job.getResult().put("variationCount", variations.size());

        if (!versionDetails.isEmpty()) {
            job.getResult().put("createdVersions", versionDetails);
        }

        job.updateProgress(100);
        appendToLog(execution, "Variation job completed successfully.");

        // Set job as completed
        job.setStatus(BatchJobStatus.COMPLETED);
        job.setCompletedAt(LocalDateTime.now());

        return job;
    }

    /**
     * Process custom job
     */
    private BatchJob processCustomJob(BatchJob job) {
        log.debug("Processing custom job: {}", job.getId());

        // Handle custom job types based on configuration
        String jobHandler = (String) job.getConfiguration().getOrDefault("handler", "default");

        // Get latest execution
        BatchJobExecution execution = job.getLatestExecution();
        appendToLog(execution, "Processing custom job with handler: " + jobHandler);

        job.updateProgress(50);
        appendToLog(execution, "Custom job processing is not implemented for this handler.");

        // For demonstration, just complete the job
        job.updateProgress(100);
        appendToLog(execution, "Custom job completed.");

        job.setStatus(BatchJobStatus.COMPLETED);
        job.setCompletedAt(LocalDateTime.now());

        return job;
    }

    /**
     * Generate random parameters for testing
     */
    private Map<String, Object> generateRandomParameters(PromptVersion version) {
        Map<String, Object> params = new HashMap<>();

        // Get version parameters
        List<String> paramNames = version.getParameters().stream()
                .map(p -> p.getName())
                .collect(Collectors.toList());

        // Generate random values
        for (String paramName : paramNames) {
            // Simple random values for testing
            params.put(paramName, "Test value for " + paramName);
        }

        return params;
    }

    /**
     * Schedule to process pending and scheduled jobs
     */
    @Scheduled(fixedDelay = 60000) // Run every minute
    @Transactional
    public void processScheduledJobs() {
        log.debug("Checking for scheduled jobs to process");

        // Get scheduled jobs that are ready to run
        List<BatchJob> scheduledJobs = jobRepository.findReadyToRunJobs(
                LocalDateTime.now(), PageRequest.of(0, MAX_CONCURRENT_JOBS));

        for (BatchJob job : scheduledJobs) {
            // Start the job
            job.setStatus(BatchJobStatus.RUNNING);
            job.setStartedAt(LocalDateTime.now());
            BatchJob updatedJob = jobRepository.save(job);

            // Process job asynchronously
            processJobAsync(updatedJob.getId());
        }
    }

    /**
     * Check for stuck jobs
     */
    @Scheduled(fixedDelay = 3600000) // Run every hour
    @Transactional
    public void checkStuckJobs() {
        log.debug("Checking for stuck jobs");

        // Get jobs that have been running for too long
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(DEFAULT_JOB_TIMEOUT_HOURS);
        List<BatchJob> stuckJobs = jobRepository.findStuckJobs(cutoffTime);

        for (BatchJob job : stuckJobs) {
            log.warn("Detected stuck job: {} (running since {})", job.getId(), job.getStartedAt());

            // Mark job as failed
            job.setStatus(BatchJobStatus.FAILED);
            job.setCompletedAt(LocalDateTime.now());
            job.setErrorMessage("Job timed out after " + DEFAULT_JOB_TIMEOUT_HOURS + " hours");

            jobRepository.save(job);
        }
    }

    /**
     * Convert job entity to response DTO
     */
    private BatchJobResponse convertToResponse(BatchJob job) {
        if (job == null) {
            return null;
        }

        // Build template info if available
        BatchJobResponse.TemplateInfo templateInfo = null;
        if (job.getTemplate() != null) {
            templateInfo = BatchJobResponse.TemplateInfo.builder()
                    .id(job.getTemplate().getId())
                    .name(job.getTemplate().getName())
                    .build();
        }

        // Build version info if available
        BatchJobResponse.VersionInfo versionInfo = null;
        if (job.getVersion() != null) {
            versionInfo = BatchJobResponse.VersionInfo.builder()
                    .id(job.getVersion().getId())
                    .versionNumber(job.getVersion().getVersionNumber())
                    .build();
        }

        // Build execution infos
        List<BatchJobResponse.ExecutionInfo> executionInfos = new ArrayList<>();
        if (job.getExecutions() != null) {
            executionInfos = job.getExecutions().stream()
                    .map(e -> BatchJobResponse.ExecutionInfo.builder()
                            .id(e.getId())
                            .status(e.getStatus())
                            .startedAt(e.getStartedAt())
                            .completedAt(e.getCompletedAt())
                            .durationMs(e.getDurationMs())
                            .workerId(e.getWorkerId())
                            .errorMessage(e.getErrorMessage())
                            .build())
                    .collect(Collectors.toList());
        }

        // Get latest execution log
        String latestExecutionLog = null;
        if (!executionInfos.isEmpty()) {
            BatchJobExecution latestExecution = job.getLatestExecution();
            if (latestExecution != null) {
                latestExecutionLog = latestExecution.getExecutionLog();
            }
        }

        // Estimate remaining time
        Long estimatedTimeRemaining = null;
        if (job.isRunning() && job.getStartedAt() != null && job.getCompletionPercentage() != null
                && job.getCompletionPercentage() > 0 && job.getCompletionPercentage() < 100) {

            LocalDateTime now = LocalDateTime.now();
            long elapsedSeconds = now.toEpochSecond(java.time.ZoneOffset.UTC)
                    - job.getStartedAt().toEpochSecond(java.time.ZoneOffset.UTC);

            if (elapsedSeconds > 0) {
                double secondsPerPercent = (double) elapsedSeconds / job.getCompletionPercentage();
                double remainingPercent = 100 - job.getCompletionPercentage();
                estimatedTimeRemaining = (long) (secondsPerPercent * remainingPercent);
            }
        }

        // Build the response
        return BatchJobResponse.builder()
                .id(job.getId())
                .name(job.getName())
                .description(job.getDescription())
                .jobType(job.getJobType())
                .status(job.getStatus())
                .createdBy(job.getCreatedBy())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .scheduledAt(job.getScheduledAt())
                .startedAt(job.getStartedAt())
                .completedAt(job.getCompletedAt())
                .priority(job.getPriority())
                .maxRetries(job.getMaxRetries())
                .retryCount(job.getRetryCount())
                .errorMessage(job.getErrorMessage())
                .completionPercentage(job.getCompletionPercentage())
                .template(templateInfo)
                .version(versionInfo)
                .parameters(job.getParameters())
                .configuration(job.getConfiguration())
                .result(job.getResult())
                .executions(executionInfos)
                .estimatedTimeRemaining(estimatedTimeRemaining)
                .latestExecutionLog(latestExecutionLog)
                .build();
    }

    /**
     * Validate job request
     */
    private void validateJobRequest(BatchJobRequest request) {
        if (request.getJobType() == null) {
            throw new ValidationException("Job type is required");
        }

        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new ValidationException("Job name is required");
        }

        // Validate based on job type
        switch (request.getJobType()) {
            case PROMPT_OPTIMIZATION:
            case PROMPT_VARIATION:
            case BATCH_EXECUTION:
                if (request.getVersionId() == null) {
                    throw new ValidationException("Version ID is required for " + request.getJobType() + " jobs");
                }
                break;

            case DATA_IMPORT:
            case DATA_EXPORT:
                if (request.getTemplateId() == null) {
                    throw new ValidationException("Template ID is required for " + request.getJobType() + " jobs");
                }
                break;

            default:
                // No specific validation for other job types
                break;
        }

        // Validate scheduling
        if (request.getScheduledAt() != null && !request.isStartImmediately()
                && request.getScheduledAt().isBefore(LocalDateTime.now())) {
            throw new ValidationException("Scheduled time must be in the future");
        }
    }

    /**
     * Get optimization request from parameters
     */
    private PromptOptimizationRequest getOptimizationRequest(Map<String, Object> parameters) {
        if (parameters == null) {
            return new PromptOptimizationRequest();
        }

        // Check for optimization request in parameters
        @SuppressWarnings("unchecked")
        Map<String, Object> optimizationRequestMap = (Map<String, Object>) parameters.get("optimizationRequest");
        if (optimizationRequestMap != null) {
            // Convert map to request
            PromptOptimizationRequest request = new PromptOptimizationRequest();

            // Set fields from map
            if (optimizationRequestMap.containsKey("applyAutomatically")) {
                request.setApplyAutomatically((Boolean) optimizationRequestMap.get("applyAutomatically"));
            }

            if (optimizationRequestMap.containsKey("createNewVersion")) {
                request.setCreateNewVersion((Boolean) optimizationRequestMap.get("createNewVersion"));
            }

            if (optimizationRequestMap.containsKey("modelId")) {
                request.setModelId((String) optimizationRequestMap.get("modelId"));
            }

            if (optimizationRequestMap.containsKey("providerId")) {
                request.setProviderId((String) optimizationRequestMap.get("providerId"));
            }

            if (optimizationRequestMap.containsKey("maxTokens")) {
                request.setMaxTokens((Integer) optimizationRequestMap.get("maxTokens"));
            }

            if (optimizationRequestMap.containsKey("measureImpact")) {
                request.setMeasureImpact((Boolean) optimizationRequestMap.get("measureImpact"));
            }

            if (optimizationRequestMap.containsKey("testSampleCount")) {
                request.setTestSampleCount((Integer) optimizationRequestMap.get("testSampleCount"));
            }

            // Handle suggestion types
            @SuppressWarnings("unchecked")
            List<String> suggestionTypeStrings = (List<String>) optimizationRequestMap.get("suggestionTypes");
            if (suggestionTypeStrings != null) {
                List<SuggestionType> suggestionTypes = suggestionTypeStrings.stream()
                        .map(SuggestionType::valueOf)
                        .collect(Collectors.toList());
                request.setSuggestionTypes(suggestionTypes);
            }

            // Handle strategies
            @SuppressWarnings("unchecked")
            Map<String, Boolean> strategies = (Map<String, Boolean>) optimizationRequestMap.get("strategies");
            if (strategies != null) {
                request.setStrategies(strategies);
            }

            // Handle parameters
            @SuppressWarnings("unchecked")
            Map<String, Object> requestParams = (Map<String, Object>) optimizationRequestMap.get("parameters");
            if (requestParams != null) {
                request.setParameters(requestParams);
            }

            return request;
        }

        // Create default request
        PromptOptimizationRequest request = new PromptOptimizationRequest();

        // Set default suggestion types
        request.setSuggestionTypes(List.of(
                SuggestionType.TOKEN_EFFICIENCY,
                SuggestionType.CLARITY,
                SuggestionType.PARAMETER_USAGE,
                SuggestionType.ERROR_HANDLING
        ));

        // Set default strategies
        Map<String, Boolean> strategies = new HashMap<>();
        strategies.put("removeRedundancy", true);
        strategies.put("improveStructure", true);
        strategies.put("addErrorHandling", true);
        strategies.put("improveParameterHandling", true);
        request.setStrategies(strategies);

        // Set other defaults
        request.setApplyAutomatically(true);
        request.setCreateNewVersion((Boolean) parameters.getOrDefault("createNewVersion", false));

        return request;
    }

    /**
     * Append to execution log
     */
    private void appendToLog(BatchJobExecution execution, String message) {
        if (execution != null) {
            execution.appendToLog(message);
        }
    }
}