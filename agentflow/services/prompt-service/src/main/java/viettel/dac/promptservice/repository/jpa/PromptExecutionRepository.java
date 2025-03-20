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
 * Repository for prompt executions
 */
@Repository
public interface PromptExecutionRepository extends JpaRepository<PromptExecution, String> {

    Page<PromptExecution> findByVersionId(String versionId, Pageable pageable);

    List<PromptExecution> findByVersionId(String versionId);

    Page<PromptExecution> findByVersionIdAndStatus(String versionId, ExecutionStatus status, Pageable pageable);

    Page<PromptExecution> findByVersionIdAndProviderId(String versionId, String providerId, Pageable pageable);

    @Query("SELECT e FROM PromptExecution e WHERE e.version.id = :versionId AND e.executedAt >= :startDate AND e.executedAt <= :endDate")
    Page<PromptExecution> findByVersionIdAndDateRange(
            @Param("versionId") String versionId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    @Query("SELECT AVG(e.responseTimeMs) FROM PromptExecution e WHERE e.version.id = :versionId AND e.status = 'SUCCESS'")
    Long getAverageResponseTime(@Param("versionId") String versionId);

    @Query("SELECT AVG(e.tokenCount) FROM PromptExecution e WHERE e.version.id = :versionId AND e.status = 'SUCCESS'")
    Double getAverageTokenCount(@Param("versionId") String versionId);

    @Query("SELECT AVG(e.cost) FROM PromptExecution e WHERE e.version.id = :versionId AND e.status = 'SUCCESS'")
    Double getAverageCost(@Param("versionId") String versionId);

    @Query("SELECT COUNT(e) FROM PromptExecution e WHERE e.version.template.id = :templateId AND e.status = :status")
    Long countByTemplateIdAndStatus(@Param("templateId") String templateId, @Param("status") ExecutionStatus status);
}