package viettel.dac.promptservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import viettel.dac.promptservice.dto.request.PromptBatchTestRequest;
import viettel.dac.promptservice.dto.request.PromptTestRequest;
import viettel.dac.promptservice.dto.response.ErrorResponse;
import viettel.dac.promptservice.dto.response.PromptExecutionResult;
import viettel.dac.promptservice.dto.validation.ValidationResult;
import viettel.dac.promptservice.service.preview.PromptTestingService;

import java.util.List;
import java.util.Map;

/**
 * REST controller for prompt testing operations
 */
@RestController
@RequestMapping("/api/v1/testing")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Prompt Testing", description = "API endpoints for testing prompts against LLM providers")
public class PromptTestingController {

    private final PromptTestingService testingService;

    @Operation(summary = "Test a prompt against an LLM provider")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Test executed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Version not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/test")
    @PreAuthorize("hasAuthority('PROMPT_TEST')")
    public ResponseEntity<PromptExecutionResult> testPrompt(
            @Valid @RequestBody PromptTestRequest request) {
        log.debug("REST request to test prompt version: {}", request.getVersionId());
        PromptExecutionResult result = testingService.testPrompt(request);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Execute batch testing of a prompt")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Batch test executed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Version not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/batch")
    @PreAuthorize("hasAuthority('PROMPT_TEST')")
    public ResponseEntity<List<PromptExecutionResult>> batchTestPrompt(
            @Valid @RequestBody PromptBatchTestRequest request) {
        log.debug("REST request to batch test prompt version: {}", request.getVersionId());
        List<PromptExecutionResult> results = testingService.batchTestPrompt(request);
        return ResponseEntity.ok(results);
    }

    @Operation(summary = "Validate a response against criteria")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Validation executed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/validate")
    @PreAuthorize("hasAuthority('PROMPT_TEST')")
    public ResponseEntity<ValidationResult> validateResponse(
            @RequestParam String response,
            @RequestBody Map<String, Object> validationCriteria) {
        log.debug("REST request to validate response against criteria");
        ValidationResult result = testingService.validateResponse(response, validationCriteria);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Compare two test executions")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Comparison executed successfully"),
            @ApiResponse(responseCode = "404", description = "Execution not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/compare")
    @PreAuthorize("hasAuthority('PROMPT_TEST')")
    public ResponseEntity<Map<String, Object>> compareResponses(
            @Parameter(description = "First execution ID") @RequestParam String executionId1,
            @Parameter(description = "Second execution ID") @RequestParam String executionId2) {
        log.debug("REST request to compare executions: {} and {}", executionId1, executionId2);
        Map<String, Object> comparison = testingService.compareResponses(executionId1, executionId2);
        return ResponseEntity.ok(comparison);
    }

    @Operation(summary = "Get test history for a prompt version")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "History retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Version not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/history/{versionId}")
    @PreAuthorize("hasAuthority('PROMPT_READ')")
    public ResponseEntity<List<PromptExecutionResult>> getTestHistory(
            @Parameter(description = "Version ID") @PathVariable String versionId,
            @Parameter(description = "Maximum number of records") @RequestParam(defaultValue = "10") int limit) {
        log.debug("REST request to get test history for version: {}", versionId);
        List<PromptExecutionResult> history = testingService.getTestHistory(versionId, limit);
        return ResponseEntity.ok(history);
    }
}