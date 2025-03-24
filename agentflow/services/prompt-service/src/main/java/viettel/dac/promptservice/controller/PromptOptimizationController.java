package viettel.dac.promptservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import viettel.dac.promptservice.dto.optimization.OptimizationResult;
import viettel.dac.promptservice.dto.optimization.PromptOptimizationRequest;
import viettel.dac.promptservice.dto.optimization.SuggestionType;
import viettel.dac.promptservice.dto.response.PromptVersionResponse;
import viettel.dac.promptservice.model.entity.PromptVersion;
import viettel.dac.promptservice.service.mapper.EntityDtoMapper;
import viettel.dac.promptservice.service.optimization.PromptOptimizationService;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for prompt optimization operations
 */
@RestController
@RequestMapping("/api/v1/optimization")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Prompt Optimization", description = "API endpoints for prompt optimization and analysis")
public class PromptOptimizationController {

    private final PromptOptimizationService optimizationService;
    private final EntityDtoMapper mapper;

    /**
     * Analyze a prompt version and provide optimization suggestions
     */
    @GetMapping("/analyze/{versionId}")
    @Operation(summary = "Analyze a prompt version for optimization opportunities")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Analysis completed successfully"),
            @ApiResponse(responseCode = "404", description = "Prompt version not found")
    })
    public ResponseEntity<OptimizationResult> analyzePrompt(
            @Parameter(description = "Prompt version ID", required = true) @PathVariable String versionId) {
        log.debug("REST request to analyze prompt version: {}", versionId);
        OptimizationResult result = optimizationService.analyzePrompt(versionId);
        return ResponseEntity.ok(result);
    }

    /**
     * Generate optimized variations of a prompt
     */
    @PostMapping("/variations/{versionId}")
    @Operation(summary = "Generate optimized variations of a prompt")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Variations generated successfully"),
            @ApiResponse(responseCode = "404", description = "Prompt version not found")
    })
    public ResponseEntity<Map<String, Map<String, Object>>> generateOptimizedVariations(
            @Parameter(description = "Prompt version ID", required = true) @PathVariable String versionId,
            @Parameter(description = "Suggestion types to apply") @RequestParam(required = false) List<SuggestionType> suggestionTypes,
            @Parameter(description = "Number of variations to generate") @RequestParam(defaultValue = "3") int count) {
        log.debug("REST request to generate optimized variations for version: {}", versionId);

        // If suggestion types not specified, use all types
        if (suggestionTypes == null || suggestionTypes.isEmpty()) {
            suggestionTypes = List.of(SuggestionType.values());
        }

        Map<String, Map<String, Object>> variations = optimizationService.generateOptimizedVariations(
                versionId, suggestionTypes, count);
        return ResponseEntity.ok(variations);
    }

    /**
     * Run a complete optimization process
     */
    @PostMapping("/optimize/{versionId}")
    @Operation(summary = "Optimize a prompt version")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Optimization completed successfully"),
            @ApiResponse(responseCode = "404", description = "Prompt version not found"),
            @ApiResponse(responseCode = "400", description = "Invalid optimization request")
    })
    public ResponseEntity<OptimizationResult> optimizePrompt(
            @Parameter(description = "Prompt version ID", required = true) @PathVariable String versionId,
            @Valid @RequestBody PromptOptimizationRequest request) {
        log.debug("REST request to optimize prompt version: {}", versionId);
        OptimizationResult result = optimizationService.optimizePromptAsync(versionId, request).join();
        return ResponseEntity.ok(result);
    }

    /**
     * Start an asynchronous optimization process
     */
    @PostMapping("/optimize-async/{versionId}")
    @Operation(summary = "Start asynchronous optimization of a prompt version")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Optimization process started"),
            @ApiResponse(responseCode = "404", description = "Prompt version not found"),
            @ApiResponse(responseCode = "400", description = "Invalid optimization request")
    })
    public ResponseEntity<Void> optimizePromptAsync(
            @Parameter(description = "Prompt version ID", required = true) @PathVariable String versionId,
            @Valid @RequestBody PromptOptimizationRequest request) {
        log.debug("REST request to asynchronously optimize prompt version: {}", versionId);
        optimizationService.optimizePromptAsync(versionId, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    /**
     * Create a new version with optimized prompt text
     */
    @PostMapping("/create-version/{sourceVersionId}")
    @Operation(summary = "Create a new version with optimized prompt text")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Optimized version created successfully"),
            @ApiResponse(responseCode = "404", description = "Source version not found"),
            @ApiResponse(responseCode = "400", description = "Invalid optimization text")
    })
    public ResponseEntity<PromptVersionResponse> createOptimizedVersion(
            @Parameter(description = "Source version ID", required = true) @PathVariable String sourceVersionId,
            @Parameter(description = "Optimized prompt text", required = true) @RequestBody String optimizedText) {
        log.debug("REST request to create optimized version from source: {}", sourceVersionId);
        PromptVersion newVersion = optimizationService.createOptimizedVersion(sourceVersionId, optimizedText);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toVersionResponse(newVersion));
    }

    /**
     * Create an optimization batch job
     */
    @PostMapping("/job/{versionId}")
    @Operation(summary = "Create an optimization batch job")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Optimization job created successfully"),
            @ApiResponse(responseCode = "404", description = "Prompt version not found"),
            @ApiResponse(responseCode = "400", description = "Invalid optimization request")
    })
    public ResponseEntity<String> createOptimizationJob(
            @Parameter(description = "Prompt version ID", required = true) @PathVariable String versionId,
            @Valid @RequestBody PromptOptimizationRequest request) {
        log.debug("REST request to create optimization job for version: {}", versionId);
        String jobId = optimizationService.createOptimizationJob(versionId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(jobId);
    }

    /**
     * Apply specific optimization strategies
     */
    @PostMapping("/strategies/{versionId}")
    @Operation(summary = "Apply specific optimization strategies")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Strategies applied successfully"),
            @ApiResponse(responseCode = "404", description = "Prompt version not found")
    })
    public ResponseEntity<String> applyOptimizationStrategies(
            @Parameter(description = "Prompt version ID", required = true) @PathVariable String versionId,
            @RequestBody Map<String, Boolean> strategies) {
        log.debug("REST request to apply optimization strategies to version: {}", versionId);
        String optimizedText = optimizationService.applyOptimizationStrategies(versionId, strategies);
        return ResponseEntity.ok(optimizedText);
    }

    /**
     * Suggest token efficiency improvements
     */
    @GetMapping("/suggest/token-efficiency/{versionId}")
    @Operation(summary = "Suggest token efficiency improvements")
    public ResponseEntity<OptimizationResult> suggestTokenEfficiencyImprovements(
            @Parameter(description = "Prompt version ID", required = true) @PathVariable String versionId) {
        log.debug("REST request to suggest token efficiency improvements for version: {}", versionId);
        OptimizationResult result = optimizationService.suggestTokenEfficiencyImprovements(versionId);
        return ResponseEntity.ok(result);
    }

    /**
     * Suggest clarity improvements
     */
    @GetMapping("/suggest/clarity/{versionId}")
    @Operation(summary = "Suggest clarity improvements")
    public ResponseEntity<OptimizationResult> suggestClarityImprovements(
            @Parameter(description = "Prompt version ID", required = true) @PathVariable String versionId) {
        log.debug("REST request to suggest clarity improvements for version: {}", versionId);
        OptimizationResult result = optimizationService.suggestClarityImprovements(versionId);
        return ResponseEntity.ok(result);
    }

    /**
     * Suggest error handling improvements
     */
    @GetMapping("/suggest/error-handling/{versionId}")
    @Operation(summary = "Suggest error handling improvements")
    public ResponseEntity<OptimizationResult> suggestErrorHandlingImprovements(
            @Parameter(description = "Prompt version ID", required = true) @PathVariable String versionId) {
        log.debug("REST request to suggest error handling improvements for version: {}", versionId);
        OptimizationResult result = optimizationService.suggestErrorHandlingImprovements(versionId);
        return ResponseEntity.ok(result);
    }

    /**
     * Suggest parameter improvements
     */
    @GetMapping("/suggest/parameters/{versionId}")
    @Operation(summary = "Suggest parameter improvements")
    public ResponseEntity<OptimizationResult> suggestParameterImprovements(
            @Parameter(description = "Prompt version ID", required = true) @PathVariable String versionId) {
        log.debug("REST request to suggest parameter improvements for version: {}", versionId);
        OptimizationResult result = optimizationService.suggestParameterImprovements(versionId);
        return ResponseEntity.ok(result);
    }
}