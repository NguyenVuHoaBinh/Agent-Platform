package viettel.dac.promptservice.repository.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import viettel.dac.promptservice.model.entity.PromptExecution;
import viettel.dac.promptservice.model.enums.ExecutionStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Enhanced repository for prompt executions
 */
@Repository
public interface PromptExecutionRepository extends JpaRepository<PromptExecution, String> {

    /**
     * Find executions by version ID
     */
    Page<PromptExecution> findByVersionId(String versionId, Pageable pageable);

    /**
     * Get all executions by version ID (non-paginated)
     */
    List<PromptExecution> findByVersionId(String versionId);

    /**
     * Find executions by version ID and status
     */
    Page<PromptExecution> findByVersionIdAndStatus(String versionId, ExecutionStatus status, Pageable pageable);

    /**
     * Find executions by version ID and provider ID
     */
    Page<PromptExecution> findByVersionIdAndProviderId(String versionId, String providerId, Pageable pageable);

    /**
     * Find executions by version ID and date range
     */
    @Query("SELECT e FROM PromptExecution e WHERE e.version.id = :versionId AND e.executedAt >= :startDate AND e.executedAt <= :endDate")
    Page<PromptExecution> findByVersionIdAndDateRange(
            @Param("versionId") String versionId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    /**
     * Get average response time for a version
     */
    @Query("SELECT AVG(e.responseTimeMs) FROM PromptExecution e WHERE e.version.id = :versionId AND e.status = 'SUCCESS'")
    Long getAverageResponseTime(@Param("versionId") String versionId);

    /**
     * Get average token count for a version
     */
    @Query("SELECT AVG(e.tokenCount) FROM PromptExecution e WHERE e.version.id = :versionId AND e.status = 'SUCCESS'")
    Double getAverageTokenCount(@Param("versionId") String versionId);

    /**
     * Get average cost for a version
     */
    @Query("SELECT AVG(e.cost) FROM PromptExecution e WHERE e.version.id = :versionId AND e.status = 'SUCCESS'")
    Double getAverageCost(@Param("versionId") String versionId);

    /**
     * Count executions by template ID and status
     */
    @Query("SELECT COUNT(e) FROM PromptExecution e WHERE e.version.template.id = :templateId AND e.status = :status")
    Long countByTemplateIdAndStatus(@Param("templateId") String templateId, @Param("status") ExecutionStatus status);

    /**
     * Find latest execution by version ID
     */
    @Query("SELECT e FROM PromptExecution e WHERE e.version.id = :versionId ORDER BY e.executedAt DESC")
    List<PromptExecution> findLatestByVersionId(@Param("versionId") String versionId, Pageable pageable);

    /**
     * Find executions by user ID
     */
    Page<PromptExecution> findByExecutedBy(String executedBy, Pageable pageable);

    /**
     * Get success rate for a version
     */
    @Query("SELECT (SUM(CASE WHEN e.status = 'SUCCESS' THEN 1 ELSE 0 END) * 1.0) / COUNT(e) " +
            "FROM PromptExecution e WHERE e.version.id = :versionId")
    Double getSuccessRate(@Param("versionId") String versionId);


    /**
     * Get average token usage by provider
     */
    @Query("SELECT e.providerId, AVG(e.tokenCount) FROM PromptExecution e " +
            "WHERE e.status = 'SUCCESS' GROUP BY e.providerId")
    List<Object[]> getAverageTokenUsageByProvider();

    /**
     * Get total cost by template
     */
    @Query("SELECT e.version.template.id, SUM(e.cost) FROM PromptExecution e " +
            "WHERE e.cost IS NOT NULL GROUP BY e.version.template.id")
    List<Object[]> getTotalCostByTemplate();

    /**
     * Get execution count by status and date range
     */
    @Query("SELECT e.status, COUNT(e) FROM PromptExecution e " +
            "WHERE e.executedAt BETWEEN :startDate AND :endDate GROUP BY e.status")
    List<Object[]> getExecutionCountByStatus(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Get executions per day for a template
     */
    @Query("SELECT FUNCTION('DATE', e.executedAt), COUNT(e) FROM PromptExecution e " +
            "WHERE e.version.template.id = :templateId " +
            "GROUP BY FUNCTION('DATE', e.executedAt) " +
            "ORDER BY FUNCTION('DATE', e.executedAt)")
    List<Object[]> getExecutionsPerDay(@Param("templateId") String templateId);

    /**
     * Find executions by status
     */
    Page<PromptExecution> findByStatus(ExecutionStatus status, Pageable pageable);

    /**
     * Count executions by status
     */
    @Query("SELECT COUNT(e) FROM PromptExecution e WHERE e.status = :status")
    long countByStatus(@Param("status") ExecutionStatus status);
}