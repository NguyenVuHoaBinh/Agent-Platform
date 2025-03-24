package viettel.dac.promptservice.repository.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import viettel.dac.promptservice.model.entity.BatchJobExecution;
import viettel.dac.promptservice.model.enums.BatchJobStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for batch job execution data
 */
@Repository
public interface BatchJobExecutionRepository extends JpaRepository<BatchJobExecution, String> {

    /**
     * Find executions by job ID
     *
     * @param jobId Job ID
     * @return List of executions
     */
    List<BatchJobExecution> findByJobId(String jobId);

    /**
     * Find executions by job ID (paginated)
     *
     * @param jobId Job ID
     * @param pageable Pagination information
     * @return Page of executions
     */
    Page<BatchJobExecution> findByJobId(String jobId, Pageable pageable);

    /**
     * Find executions by status
     *
     * @param status Execution status
     * @param pageable Pagination information
     * @return Page of executions
     */
    Page<BatchJobExecution> findByStatus(BatchJobStatus status, Pageable pageable);

    /**
     * Find latest execution for a job
     *
     * @param jobId Job ID
     * @return Optional containing the latest execution
     */
    @Query("SELECT e FROM BatchJobExecution e WHERE e.job.id = :jobId ORDER BY e.startedAt DESC")
    Optional<BatchJobExecution> findLatestExecution(@Param("jobId") String jobId);

    /**
     * Find currently running execution for a job
     *
     * @param jobId Job ID
     * @return Optional containing the running execution
     */
    Optional<BatchJobExecution> findByJobIdAndStatus(String jobId, BatchJobStatus status);

    /**
     * Find executions started in a specific time range
     *
     * @param startTime Start of time range
     * @param endTime End of time range
     * @param pageable Pagination information
     * @return Page of executions
     */
    Page<BatchJobExecution> findByStartedAtBetween(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * Find executions by worker ID
     *
     * @param workerId Worker ID
     * @param pageable Pagination information
     * @return Page of executions
     */
    Page<BatchJobExecution> findByWorkerId(String workerId, Pageable pageable);

    /**
     * Get average execution duration by job type
     *
     * @return List of job type and average duration pairs
     */
    @Query("SELECT j.jobType, AVG(e.durationMs) FROM BatchJobExecution e JOIN e.job j WHERE e.status = 'COMPLETED' GROUP BY j.jobType")
    List<Object[]> getAverageDurationByJobType();

    /**
     * Count executions by job ID and status
     *
     * @param jobId Job ID
     * @param status Execution status
     * @return Count of executions
     */
    long countByJobIdAndStatus(String jobId, BatchJobStatus status);

    /**
     * Find executions that are stuck in running state
     *
     * @param cutoffTime Executions started before this time are considered stuck
     * @return List of stuck executions
     */
    @Query("SELECT e FROM BatchJobExecution e WHERE e.status = 'RUNNING' AND e.startedAt < :cutoffTime")
    List<BatchJobExecution> findStuckExecutions(@Param("cutoffTime") LocalDateTime cutoffTime);
}