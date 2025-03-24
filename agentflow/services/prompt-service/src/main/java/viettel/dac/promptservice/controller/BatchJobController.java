package viettel.dac.promptservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import viettel.dac.promptservice.dto.request.BatchJobRequest;
import viettel.dac.promptservice.dto.request.BatchJobUpdateRequest;
import viettel.dac.promptservice.dto.response.BatchJobResponse;
import viettel.dac.promptservice.dto.response.PageResponse;
import viettel.dac.promptservice.exception.ResourceNotFoundException;
import viettel.dac.promptservice.model.entity.BatchJobExecution;
import viettel.dac.promptservice.model.enums.BatchJobStatus;
import viettel.dac.promptservice.model.enums.BatchJobType;
import viettel.dac.promptservice.service.batch.BatchJobService;
import viettel.dac.promptservice.service.mapper.EntityDtoMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * REST Controller for batch job operations
 */
@RestController
@RequestMapping("/api/v1/batch-jobs")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Batch Jobs", description = "API endpoints for batch job management")
public class BatchJobController {

    private final BatchJobService batchJobService;
    private final EntityDtoMapper mapper;

    /**
     * Create a new batch job
     */
    @PostMapping
    @Operation(summary = "Create a new batch job")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Job created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body")
    })
    public ResponseEntity<BatchJobResponse> createJob(@Valid @RequestBody BatchJobRequest request) {
        log.debug("REST request to create batch job: {}", request.getName());
        String jobId = batchJobService.createJob(request);
        BatchJobResponse response = batchJobService.getJobById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found after creation: " + jobId));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get a batch job by ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get a batch job by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Job found"),
            @ApiResponse(responseCode = "404", description = "Job not found")
    })
    public ResponseEntity<BatchJobResponse> getJobById(
            @Parameter(description = "Job ID", required = true) @PathVariable String id) {
        log.debug("REST request to get batch job: {}", id);
        return batchJobService.getJobById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + id));
    }

    /**
     * Update a batch job
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update a batch job")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Job updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "404", description = "Job not found")
    })
    public ResponseEntity<BatchJobResponse> updateJob(
            @Parameter(description = "Job ID", required = true) @PathVariable String id,
            @Valid @RequestBody BatchJobUpdateRequest request) {
        log.debug("REST request to update batch job: {}", id);
        BatchJobResponse response = batchJobService.updateJob(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a batch job
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a batch job")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Job deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Job not found")
    })
    public ResponseEntity<Void> deleteJob(
            @Parameter(description = "Job ID", required = true) @PathVariable String id) {
        log.debug("REST request to delete batch job: {}", id);
        batchJobService.deleteJob(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Start a batch job
     */
    @PostMapping("/{id}/start")
    @Operation(summary = "Start a batch job")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Job started successfully"),
            @ApiResponse(responseCode = "404", description = "Job not found"),
            @ApiResponse(responseCode = "400", description = "Job cannot be started (wrong state)")
    })
    public ResponseEntity<BatchJobResponse> startJob(
            @Parameter(description = "Job ID", required = true) @PathVariable String id) {
        log.debug("REST request to start batch job: {}", id);
        BatchJobResponse response = batchJobService.startJob(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Cancel a batch job
     */
    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a batch job")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Job cancelled successfully"),
            @ApiResponse(responseCode = "404", description = "Job not found"),
            @ApiResponse(responseCode = "400", description = "Job cannot be cancelled (wrong state)")
    })
    public ResponseEntity<BatchJobResponse> cancelJob(
            @Parameter(description = "Job ID", required = true) @PathVariable String id) {
        log.debug("REST request to cancel batch job: {}", id);
        BatchJobResponse response = batchJobService.cancelJob(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Retry a failed batch job
     */
    @PostMapping("/{id}/retry")
    @Operation(summary = "Retry a failed batch job")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Job retry initiated successfully"),
            @ApiResponse(responseCode = "404", description = "Job not found"),
            @ApiResponse(responseCode = "400", description = "Job cannot be retried (wrong state)")
    })
    public ResponseEntity<BatchJobResponse> retryJob(
            @Parameter(description = "Job ID", required = true) @PathVariable String id) {
        log.debug("REST request to retry batch job: {}", id);
        BatchJobResponse response = batchJobService.retryJob(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Schedule a batch job
     */
    @PostMapping("/{id}/schedule")
    @Operation(summary = "Schedule a batch job to run at a specific time")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Job scheduled successfully"),
            @ApiResponse(responseCode = "404", description = "Job not found"),
            @ApiResponse(responseCode = "400", description = "Job cannot be scheduled (wrong state or invalid time)")
    })
    public ResponseEntity<BatchJobResponse> scheduleJob(
            @Parameter(description = "Job ID", required = true) @PathVariable String id,
            @Parameter(description = "Scheduled time (ISO format)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime scheduledTime) {
        log.debug("REST request to schedule batch job: {} at {}", id, scheduledTime);
        BatchJobResponse response = batchJobService.scheduleJob(id, scheduledTime);
        return ResponseEntity.ok(response);
    }

    /**
     * Get jobs by status
     */
    @GetMapping("/status/{status}")
    @Operation(summary = "Get batch jobs by status")
    public ResponseEntity<PageResponse<BatchJobResponse>> getJobsByStatus(
            @Parameter(description = "Job status", required = true) @PathVariable BatchJobStatus status,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        log.debug("REST request to get batch jobs by status: {}", status);
        Page<BatchJobResponse> page = batchJobService.getJobsByStatus(status, pageable);
        return ResponseEntity.ok(mapper.toPageResponse(page, job -> job));
    }

    /**
     * Get jobs by type
     */
    @GetMapping("/type/{jobType}")
    @Operation(summary = "Get batch jobs by type")
    public ResponseEntity<PageResponse<BatchJobResponse>> getJobsByType(
            @Parameter(description = "Job type", required = true) @PathVariable BatchJobType jobType,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        log.debug("REST request to get batch jobs by type: {}", jobType);
        Page<BatchJobResponse> page = batchJobService.getJobsByType(jobType, pageable);
        return ResponseEntity.ok(mapper.toPageResponse(page, job -> job));
    }

    /**
     * Get jobs by creator
     */
    @GetMapping("/creator/{createdBy}")
    @Operation(summary = "Get batch jobs by creator")
    public ResponseEntity<PageResponse<BatchJobResponse>> getJobsByCreator(
            @Parameter(description = "Creator ID", required = true) @PathVariable String createdBy,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        log.debug("REST request to get batch jobs by creator: {}", createdBy);
        Page<BatchJobResponse> page = batchJobService.getJobsByCreator(createdBy, pageable);
        return ResponseEntity.ok(mapper.toPageResponse(page, job -> job));
    }

    /**
     * Search jobs by name
     */
    @GetMapping("/search")
    @Operation(summary = "Search batch jobs by name")
    public ResponseEntity<PageResponse<BatchJobResponse>> searchJobsByName(
            @Parameter(description = "Name pattern", required = true) @RequestParam String name,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        log.debug("REST request to search batch jobs by name: {}", name);
        Page<BatchJobResponse> page = batchJobService.searchJobsByName(name, pageable);
        return ResponseEntity.ok(mapper.toPageResponse(page, job -> job));
    }

    /**
     * Get job executions
     */
    @GetMapping("/{id}/executions")
    @Operation(summary = "Get executions for a batch job")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Executions found"),
            @ApiResponse(responseCode = "404", description = "Job not found")
    })
    public ResponseEntity<List<BatchJobExecution>> getJobExecutions(
            @Parameter(description = "Job ID", required = true) @PathVariable String id,
            @PageableDefault(size = 20, sort = "startedAt") Pageable pageable) {
        log.debug("REST request to get executions for batch job: {}", id);
        List<BatchJobExecution> executions = batchJobService.getJobExecutions(id, pageable);
        return ResponseEntity.ok(executions);
    }

    /**
     * Get job statistics
     */
    @GetMapping("/statistics")
    @Operation(summary = "Get batch job statistics")
    public ResponseEntity<Map<String, Object>> getJobStatistics() {
        log.debug("REST request to get batch job statistics");
        Map<String, Object> statistics = batchJobService.getJobStatistics();
        return ResponseEntity.ok(statistics);
    }

    /**
     * Process a job asynchronously
     */
    @PostMapping("/{id}/process-async")
    @Operation(summary = "Process a batch job asynchronously")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Job processing initiated"),
            @ApiResponse(responseCode = "404", description = "Job not found")
    })
    public ResponseEntity<Void> processJobAsync(
            @Parameter(description = "Job ID", required = true) @PathVariable String id) {
        log.debug("REST request to process batch job asynchronously: {}", id);
        CompletableFuture<BatchJobResponse> future = batchJobService.processJobAsync(id);
        // Return 202 Accepted as the processing continues asynchronously
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
}