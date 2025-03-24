package viettel.dac.promptservice.repository.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import viettel.dac.promptservice.model.entity.BatchJob;
import viettel.dac.promptservice.model.enums.BatchJobStatus;
import viettel.dac.promptservice.model.enums.BatchJobType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for batch job data
 */
@Repository
public interface BatchJobRepository extends JpaRepository<BatchJob, String> {

    /**
     * Find job by ID with executions loaded
     *
     * @param id Job ID
     * @return Job with executions loaded
     */
    @Query("SELECT j FROM BatchJob j LEFT JOIN FETCH j.executions WHERE j.id = :id")
    Optional<BatchJob> findByIdWithExecutions(@Param("id") String id);

    /**
     * Find jobs by status
     *
     * @param status Job status
     * @param pageable Pagination information
     * @return Page of jobs
     */
    Page<BatchJob> findByStatus(BatchJobStatus status, Pageable pageable);

    /**
     * Find jobs by type
     *
     * @param jobType Job type
     * @param pageable Pagination information
     * @return Page of jobs
     */
    Page<BatchJob> findByJobType(BatchJobType jobType, Pageable pageable);

    /**
     * Find jobs by creator
     *
     * @param createdBy Creator ID
     * @param pageable Pagination information
     * @return Page of jobs
     */
    Page<BatchJob> findByCreatedBy(String createdBy, Pageable pageable);

    /**
     * Find jobs by template ID
     *
     * @param templateId Template ID
     * @param pageable Pagination information
     * @return Page of jobs
     */
    Page<BatchJob> findByTemplateId(String templateId, Pageable pageable);

    /**
     * Find jobs by version ID
     *
     * @param versionId Version ID
     * @param pageable Pagination information
     * @return Page of jobs
     */
    Page<BatchJob> findByVersionId(String versionId, Pageable pageable);

    /**
     * Find jobs scheduled before a specific time
     *
     * @param time Cutoff time
     * @return List of jobs
     */
    List<BatchJob> findByStatusAndScheduledAtBefore(BatchJobStatus status, LocalDateTime time);

    /**
     * Find jobs by name (partial match)
     *
     * @param name Name pattern
     * @param pageable Pagination information
     * @return Page of jobs
     */
    Page<BatchJob> findByNameContainingIgnoreCase(String name, Pageable pageable);

    /**
     * Count jobs by status
     *
     * @param status Job status
     * @return Count of jobs
     */
    long countByStatus(BatchJobStatus status);

    /**
     * Count jobs by type
     *
     * @param jobType Job type
     * @return Count of jobs
     */
    long countByJobType(BatchJobType jobType);

    /**
     * Find jobs that are ready to run
     *
     * @param limit Maximum number of jobs
     * @return List of jobs
     */
    @Query("SELECT j FROM BatchJob j WHERE j.status = 'PENDING' OR (j.status = 'SCHEDULED' AND j.scheduledAt <= :now) ORDER BY j.priority DESC, j.createdAt ASC")
    List<BatchJob> findReadyToRunJobs(@Param("now") LocalDateTime now, Pageable limit);

    /**
     * Find jobs that are stuck in running state
     *
     * @param cutoffTime Jobs started before this time are considered stuck
     * @return List of stuck jobs
     */
    @Query("SELECT j FROM BatchJob j WHERE j.status = 'RUNNING' AND j.startedAt < :cutoffTime")
    List<BatchJob> findStuckJobs(@Param("cutoffTime") LocalDateTime cutoffTime);
}