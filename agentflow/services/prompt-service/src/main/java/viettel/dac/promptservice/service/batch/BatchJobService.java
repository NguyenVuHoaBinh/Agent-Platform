package viettel.dac.promptservice.service.batch;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import viettel.dac.promptservice.dto.request.BatchJobRequest;
import viettel.dac.promptservice.dto.request.BatchJobUpdateRequest;
import viettel.dac.promptservice.dto.response.BatchJobResponse;
import viettel.dac.promptservice.model.entity.BatchJob;
import viettel.dac.promptservice.model.entity.BatchJobExecution;
import viettel.dac.promptservice.model.enums.BatchJobStatus;
import viettel.dac.promptservice.model.enums.BatchJobType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Service for batch job management
 */
public interface BatchJobService {

    /**
     * Create a new batch job
     *
     * @param request Job creation request
     * @return Job ID
     */
    String createJob(BatchJobRequest request);

    /**
     * Get a job by ID
     *
     * @param jobId Job ID
     * @return Job response if found
     */
    Optional<BatchJobResponse> getJobById(String jobId);

    /**
     * Update an existing job
     *
     * @param jobId Job ID
     * @param request Job update request
     * @return Updated job response
     */
    BatchJobResponse updateJob(String jobId, BatchJobUpdateRequest request);

    /**
     * Delete a job
     *
     * @param jobId Job ID
     */
    void deleteJob(String jobId);

    /**
     * Start a job
     *
     * @param jobId Job ID
     * @return Updated job response
     */
    BatchJobResponse startJob(String jobId);

    /**
     * Cancel a job
     *
     * @param jobId Job ID
     * @return Updated job response
     */
    BatchJobResponse cancelJob(String jobId);

    /**
     * Get jobs by status
     *
     * @param status Job status
     * @param pageable Pagination information
     * @return Page of job responses
     */
    Page<BatchJobResponse> getJobsByStatus(BatchJobStatus status, Pageable pageable);

    /**
     * Get jobs by type
     *
     * @param jobType Job type
     * @param pageable Pagination information
     * @return Page of job responses
     */
    Page<BatchJobResponse> getJobsByType(BatchJobType jobType, Pageable pageable);

    /**
     * Get jobs by creator
     *
     * @param createdBy Creator ID
     * @param pageable Pagination information
     * @return Page of job responses
     */
    Page<BatchJobResponse> getJobsByCreator(String createdBy, Pageable pageable);

    /**
     * Search jobs by name
     *
     * @param name Name pattern
     * @param pageable Pagination information
     * @return Page of job responses
     */
    Page<BatchJobResponse> searchJobsByName(String name, Pageable pageable);

    /**
     * Get job executions
     *
     * @param jobId Job ID
     * @param pageable Pagination information
     * @return List of executions
     */
    List<BatchJobExecution> getJobExecutions(String jobId, Pageable pageable);

    /**
     * Process a batch job
     *
     * @param job Job entity
     * @return Updated job entity
     */
    BatchJob processJob(BatchJob job);

    /**
     * Process a job asynchronously
     *
     * @param jobId Job ID
     * @return Completable future with the job response
     */
    CompletableFuture<BatchJobResponse> processJobAsync(String jobId);

    /**
     * Schedule a job to run at a specific time
     *
     * @param jobId Job ID
     * @param scheduledTime Time to run the job
     * @return Updated job response
     */
    BatchJobResponse scheduleJob(String jobId, LocalDateTime scheduledTime);

    /**
     * Get dashboard statistics about jobs
     *
     * @return Map of statistics
     */
    Map<String, Object> getJobStatistics();

    /**
     * Retry a failed job
     *
     * @param jobId Job ID
     * @return Updated job response
     */
    BatchJobResponse retryJob(String jobId);

    /**
     * Process optimization job
     *
     * @param job Optimization job
     * @return Updated job entity
     */
    BatchJob processOptimizationJob(BatchJob job);

    /**
     * Process batch execution job
     *
     * @param job Batch execution job
     * @return Updated job entity
     */
    BatchJob processBatchExecutionJob(BatchJob job);
}