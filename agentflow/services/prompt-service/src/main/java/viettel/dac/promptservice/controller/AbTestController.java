package viettel.dac.promptservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import viettel.dac.promptservice.dto.request.AbTestRequest;
import viettel.dac.promptservice.dto.request.AbTestUpdateRequest;
import viettel.dac.promptservice.dto.response.AbTestResponse;
import viettel.dac.promptservice.dto.response.PageResponse;
import viettel.dac.promptservice.exception.ResourceNotFoundException;
import viettel.dac.promptservice.model.enums.TestStatus;
import viettel.dac.promptservice.service.mapper.EntityDtoMapper;
import viettel.dac.promptservice.service.testing.AbTestService;

import java.time.LocalDateTime;

/**
 * REST controller for A/B testing functionality
 */
@RestController
@RequestMapping("/api/v1/tests")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "A/B Tests", description = "A/B testing endpoints")
public class AbTestController {

    private final AbTestService testService;
    private final EntityDtoMapper mapper;

    @PostMapping
    @Operation(summary = "Create a new A/B test",
            description = "Creates a new test comparing two prompt versions")
    public ResponseEntity<AbTestResponse> createTest(@Valid @RequestBody AbTestRequest request) {
        log.debug("POST /api/v1/tests with request: {}", request);

        AbTestResponse response = testService.createTest(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{testId}")
    @Operation(summary = "Get test by ID",
            description = "Returns a test by its ID with results")
    public ResponseEntity<AbTestResponse> getTest(@PathVariable String testId) {
        log.debug("GET /api/v1/tests/{}", testId);

        AbTestResponse response = testService.getTestById(testId)
                .orElseThrow(() -> new ResourceNotFoundException("Test not found with id: " + testId));

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{testId}")
    @Operation(summary = "Update a test",
            description = "Updates an existing test")
    public ResponseEntity<AbTestResponse> updateTest(
            @PathVariable String testId,
            @Valid @RequestBody AbTestUpdateRequest request) {

        log.debug("PUT /api/v1/tests/{}", testId);

        AbTestResponse response = testService.updateTest(testId, request);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{testId}")
    @Operation(summary = "Delete a test",
            description = "Deletes a test by ID")
    public ResponseEntity<Void> deleteTest(@PathVariable String testId) {
        log.debug("DELETE /api/v1/tests/{}", testId);

        testService.deleteTest(testId);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{testId}/start")
    @Operation(summary = "Start a test",
            description = "Starts a test that is in CREATED or PAUSED status")
    public ResponseEntity<AbTestResponse> startTest(@PathVariable String testId) {
        log.debug("POST /api/v1/tests/{}/start", testId);

        AbTestResponse response = testService.startTest(testId);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{testId}/pause")
    @Operation(summary = "Pause a test",
            description = "Pauses a running test")
    public ResponseEntity<AbTestResponse> pauseTest(@PathVariable String testId) {
        log.debug("POST /api/v1/tests/{}/pause", testId);

        AbTestResponse response = testService.pauseTest(testId);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{testId}/resume")
    @Operation(summary = "Resume a test",
            description = "Resumes a paused test")
    public ResponseEntity<AbTestResponse> resumeTest(@PathVariable String testId) {
        log.debug("POST /api/v1/tests/{}/resume", testId);

        AbTestResponse response = testService.resumeTest(testId);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{testId}/complete")
    @Operation(summary = "Complete a test",
            description = "Marks a test as complete and calculates final statistics")
    public ResponseEntity<AbTestResponse> completeTest(@PathVariable String testId) {
        log.debug("POST /api/v1/tests/{}/complete", testId);

        AbTestResponse response = testService.completeTest(testId);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{testId}/cancel")
    @Operation(summary = "Cancel a test",
            description = "Cancels a test that is not yet completed")
    public ResponseEntity<AbTestResponse> cancelTest(@PathVariable String testId) {
        log.debug("POST /api/v1/tests/{}/cancel", testId);

        AbTestResponse response = testService.cancelTest(testId);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "List tests",
            description = "Returns a paginated list of tests with optional filtering")
    public ResponseEntity<PageResponse<AbTestResponse>> listTests(
            @RequestParam(required = false) @Parameter(description = "Test status") TestStatus status,
            @RequestParam(required = false) @Parameter(description = "Creator ID") String createdBy,
            @RequestParam(required = false) @Parameter(description = "Name search") String name,
            @RequestParam(required = false) @Parameter(description = "Template ID") String templateId,
            @RequestParam(required = false) @Parameter(description = "Version ID") String versionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            @Parameter(description = "Start date") LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            @Parameter(description = "End date") LocalDateTime endDate,
            @RequestParam(defaultValue = "0") @Parameter(description = "Page number") int page,
            @RequestParam(defaultValue = "10") @Parameter(description = "Page size") int size,
            @RequestParam(defaultValue = "createdAt") @Parameter(description = "Sort field") String sort,
            @RequestParam(defaultValue = "desc") @Parameter(description = "Sort direction") String direction) {

        log.debug("GET /api/v1/tests");

        // Create pageable
        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));

        // Apply filters
        Page<AbTestResponse> testsPage;

        if (status != null) {
            testsPage = testService.getTestsByStatus(status, pageable);
        } else if (createdBy != null) {
            testsPage = testService.getTestsByCreator(createdBy, pageable);
        } else if (name != null) {
            testsPage = testService.searchTestsByName(name, pageable);
        } else if (templateId != null) {
            testsPage = testService.getTestsByTemplateId(templateId, pageable);
        } else if (versionId != null) {
            testsPage = testService.getTestsByVersionId(versionId, pageable);
        } else if (startDate != null && endDate != null) {
            testsPage = testService.getTestsByDateRange(startDate, endDate, pageable);
        } else {
            // No filters, return all tests
            testsPage = testService.getTestsByStatus(null, pageable);
        }

        return ResponseEntity.ok(mapper.toPageResponse(testsPage, test -> test));
    }

    @PostMapping("/{testId}/statistics")
    @Operation(summary = "Calculate test statistics",
            description = "Recalculates statistics for a test")
    public ResponseEntity<AbTestResponse> calculateStatistics(@PathVariable String testId) {
        log.debug("POST /api/v1/tests/{}/statistics", testId);

        AbTestResponse response = testService.calculateTestStatistics(testId);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{testId}/apply")
    @Operation(summary = "Apply test winner",
            description = "Publishes the winning version from the test")
    public ResponseEntity<String> applyTestWinner(@PathVariable String testId) {
        log.debug("POST /api/v1/tests/{}/apply", testId);

        String winnerId = testService.applyTestWinner(testId);

        return ResponseEntity.ok(winnerId);
    }
}