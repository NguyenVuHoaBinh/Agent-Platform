package viettel.dac.promptservice.repository.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import viettel.dac.promptservice.model.entity.AbTest;
import viettel.dac.promptservice.model.enums.TestStatus;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repository for A/B test data
 */
@Repository
public interface AbTestRepository extends JpaRepository<AbTest, String> {

    /**
     * Find test by ID with results loaded
     *
     * @param id Test ID
     * @return Test with results loaded
     */
    @Query("SELECT t FROM AbTest t LEFT JOIN FETCH t.results WHERE t.id = :id")
    Optional<AbTest> findByIdWithResults(@Param("id") String id);

    /**
     * Find tests by status
     *
     * @param status Test status
     * @param pageable Pagination information
     * @return Page of tests
     */
    Page<AbTest> findByStatus(TestStatus status, Pageable pageable);

    /**
     * Find tests by creator
     *
     * @param createdBy Creator ID
     * @param pageable Pagination information
     * @return Page of tests
     */
    Page<AbTest> findByCreatedBy(String createdBy, Pageable pageable);

    /**
     * Find tests by template ID (control or variant)
     *
     * @param templateId Template ID
     * @param pageable Pagination information
     * @return Page of tests
     */
    @Query("SELECT t FROM AbTest t WHERE t.controlVersion.template.id = :templateId OR t.variantVersion.template.id = :templateId")
    Page<AbTest> findByTemplateId(@Param("templateId") String templateId, Pageable pageable);

    /**
     * Find tests by version ID (control or variant)
     *
     * @param versionId Version ID
     * @param pageable Pagination information
     * @return Page of tests
     */
    @Query("SELECT t FROM AbTest t WHERE t.controlVersion.id = :versionId OR t.variantVersion.id = :versionId")
    Page<AbTest> findByVersionId(@Param("versionId") String versionId, Pageable pageable);

    /**
     * Find tests by completion status
     *
     * @param completed Whether tests are completed
     * @param pageable Pagination information
     * @return Page of tests
     */
    @Query("SELECT t FROM AbTest t WHERE (:completed = true AND t.status IN ('COMPLETED', 'CANCELLED')) OR (:completed = false AND t.status NOT IN ('COMPLETED', 'CANCELLED'))")
    Page<AbTest> findByCompleted(@Param("completed") boolean completed, Pageable pageable);

    /**
     * Find tests by date range
     *
     * @param startDate Start date
     * @param endDate End date
     * @param pageable Pagination information
     * @return Page of tests
     */
    @Query("SELECT t FROM AbTest t WHERE t.createdAt BETWEEN :startDate AND :endDate")
    Page<AbTest> findByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    /**
     * Find tests by name (partial match)
     *
     * @param name Name pattern
     * @param pageable Pagination information
     * @return Page of tests
     */
    Page<AbTest> findByNameContainingIgnoreCase(String name, Pageable pageable);

    /**
     * Count tests by status
     *
     * @param status Test status
     * @return Count of tests
     */
    long countByStatus(TestStatus status);
}