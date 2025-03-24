package viettel.dac.promptservice.service.testing;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import viettel.dac.promptservice.dto.request.AbTestRequest;
import viettel.dac.promptservice.dto.request.AbTestUpdateRequest;
import viettel.dac.promptservice.dto.response.AbTestResponse;
import viettel.dac.promptservice.model.entity.AbTest;
import viettel.dac.promptservice.model.enums.TestStatus;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Service for A/B testing functionality
 */
public interface AbTestService {

    /**
     * Create a new A/B test
     *
     * @param request Test creation request
     * @return Created test response
     */
    AbTestResponse createTest(AbTestRequest request);

    /**
     * Get a test by ID
     *
     * @param testId Test ID
     * @return Test response if found
     */
    Optional<AbTestResponse> getTestById(String testId);

    /**
     * Update an existing test
     *
     * @param testId Test ID
     * @param request Test update request
     * @return Updated test response
     */
    AbTestResponse updateTest(String testId, AbTestUpdateRequest request);

    /**
     * Delete a test
     *
     * @param testId Test ID
     */
    void deleteTest(String testId);

    /**
     * Start a test
     *
     * @param testId Test ID
     * @return Updated test response
     */
    AbTestResponse startTest(String testId);

    /**
     * Pause a running test
     *
     * @param testId Test ID
     * @return Updated test response
     */
    AbTestResponse pauseTest(String testId);

    /**
     * Resume a paused test
     *
     * @param testId Test ID
     * @return Updated test response
     */
    AbTestResponse resumeTest(String testId);

    /**
     * Complete a test
     *
     * @param testId Test ID
     * @return Updated test response
     */
    AbTestResponse completeTest(String testId);

    /**
     * Cancel a test
     *
     * @param testId Test ID
     * @return Updated test response
     */
    AbTestResponse cancelTest(String testId);

    /**
     * Get tests by status
     *
     * @param status Test status
     * @param pageable Pagination information
     * @return Page of test responses
     */
    Page<AbTestResponse> getTestsByStatus(TestStatus status, Pageable pageable);

    /**
     * Get tests by creator
     *
     * @param createdBy Creator ID
     * @param pageable Pagination information
     * @return Page of test responses
     */
    Page<AbTestResponse> getTestsByCreator(String createdBy, Pageable pageable);

    /**
     * Search tests by name
     *
     * @param name Name pattern
     * @param pageable Pagination information
     * @return Page of test responses
     */
    Page<AbTestResponse> searchTestsByName(String name, Pageable pageable);

    /**
     * Get tests by template ID
     *
     * @param templateId Template ID
     * @param pageable Pagination information
     * @return Page of test responses
     */
    Page<AbTestResponse> getTestsByTemplateId(String templateId, Pageable pageable);

    /**
     * Get tests by version ID
     *
     * @param versionId Version ID
     * @param pageable Pagination information
     * @return Page of test responses
     */
    Page<AbTestResponse> getTestsByVersionId(String versionId, Pageable pageable);

    /**
     * Get tests by date range
     *
     * @param startDate Start date
     * @param endDate End date
     * @param pageable Pagination information
     * @return Page of test responses
     */
    Page<AbTestResponse> getTestsByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    /**
     * Run a single test iteration
     *
     * @param test Test entity
     * @return Test entity with updated results
     */
    AbTest runTestIteration(AbTest test);

    /**
     * Run a test asynchronously
     *
     * @param testId Test ID
     * @return Completable future with the test response
     */
    CompletableFuture<AbTestResponse> runTestAsync(String testId);

    /**
     * Apply the winning version from a test
     *
     * @param testId Test ID
     * @return Applied version ID
     */
    String applyTestWinner(String testId);

    /**
     * Calculate test statistics
     *
     * @param testId Test ID
     * @return Updated test response with statistics
     */
    AbTestResponse calculateTestStatistics(String testId);
}