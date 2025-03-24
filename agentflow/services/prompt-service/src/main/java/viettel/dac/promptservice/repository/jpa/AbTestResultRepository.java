package viettel.dac.promptservice.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import viettel.dac.promptservice.model.entity.AbTestResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repository for A/B test results
 */
@Repository
public interface AbTestResultRepository extends JpaRepository<AbTestResult, String> {

    /**
     * Find results by test ID
     *
     * @param testId Test ID
     * @return List of results
     */
    List<AbTestResult> findByTestId(String testId);

    /**
     * Find result by test ID and version ID
     *
     * @param testId Test ID
     * @param versionId Version ID
     * @return Optional result
     */
    Optional<AbTestResult> findByTestIdAndVersionId(String testId, String versionId);

    /**
     * Find control result for a test
     *
     * @param testId Test ID
     * @return Optional control result
     */
    Optional<AbTestResult> findByTestIdAndControlVersionTrue(String testId);

    /**
     * Find variant result for a test
     *
     * @param testId Test ID
     * @return Optional variant result
     */
    Optional<AbTestResult> findByTestIdAndControlVersionFalse(String testId);

    /**
     * Get average success rate for a version across all tests
     *
     * @param versionId Version ID
     * @return Average success rate
     */
    @Query("SELECT AVG(r.successRate) FROM AbTestResult r WHERE r.version.id = :versionId")
    Double getAverageSuccessRateForVersion(@Param("versionId") String versionId);

    /**
     * Get average response time for a version across all tests
     *
     * @param versionId Version ID
     * @return Average response time
     */
    @Query("SELECT AVG(r.averageResponseTime) FROM AbTestResult r WHERE r.version.id = :versionId")
    Double getAverageResponseTimeForVersion(@Param("versionId") String versionId);

    /**
     * Get average cost for a version across all tests
     *
     * @param versionId Version ID
     * @return Average cost
     */
    @Query("SELECT AVG(r.averageCost) FROM AbTestResult r WHERE r.version.id = :versionId")
    BigDecimal getAverageCostForVersion(@Param("versionId") String versionId);

    /**
     * Count tests where a version performed better than its comparison
     *
     * @param versionId Version ID
     * @return Count of tests
     */
    @Query("SELECT COUNT(r) FROM AbTestResult r " +
            "WHERE r.version.id = :versionId " +
            "AND r.test.id IN (" +
            "    SELECT r2.test.id FROM AbTestResult r2 " +
            "    WHERE r2.test.id = r.test.id " +
            "    AND r2.version.id != :versionId " +
            "    AND r2.successRate < r.successRate" +
            ")")
    long countTestsWhereVersionPerformedBetter(@Param("versionId") String versionId);

    /**
     * Count total tests for a version
     *
     * @param versionId Version ID
     * @return Count of tests
     */
    @Query("SELECT COUNT(r) FROM AbTestResult r WHERE r.version.id = :versionId")
    long countTestsForVersion(@Param("versionId") String versionId);
}