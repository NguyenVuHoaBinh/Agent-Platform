package viettel.dac.promptservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import viettel.dac.promptservice.dto.analytics.MetricsResponse;
import viettel.dac.promptservice.dto.analytics.TemplateUsageStats;
import viettel.dac.promptservice.dto.analytics.VersionPerformanceStats;
import viettel.dac.promptservice.service.analytics.PromptAnalyticsService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * REST controller for prompt analytics features
 */
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Analytics", description = "Analytics and metrics endpoints")
public class PromptAnalyticsController {

    private final PromptAnalyticsService analyticsService;

    // Template analytics endpoints

    @GetMapping("/templates/{templateId}/usage")
    @Operation(summary = "Get usage statistics for a template",
            description = "Returns usage metrics for the specified template with optional date filtering")
    public ResponseEntity<MetricsResponse<TemplateUsageStats>> getTemplateUsage(
            @PathVariable String templateId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            @Parameter(description = "Start date for filtering (ISO format)") LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            @Parameter(description = "End date for filtering (ISO format)") LocalDateTime endDate) {

        log.debug("GET /api/v1/analytics/templates/{}/usage", templateId);

        MetricsResponse<TemplateUsageStats> response =
                analyticsService.getTemplateUsageStats(templateId, startDate, endDate);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/templates/usage/bulk")
    @Operation(summary = "Get usage statistics for multiple templates",
            description = "Returns usage metrics for multiple templates in a single request")
    public ResponseEntity<MetricsResponse<Map<String, TemplateUsageStats>>> getMultipleTemplateUsage(
            @RequestBody List<String> templateIds,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            @Parameter(description = "Start date for filtering (ISO format)") LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            @Parameter(description = "End date for filtering (ISO format)") LocalDateTime endDate) {

        log.debug("POST /api/v1/analytics/templates/usage/bulk with {} templates", templateIds.size());

        MetricsResponse<Map<String, TemplateUsageStats>> response =
                analyticsService.getMultipleTemplateUsageStats(templateIds, startDate, endDate);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/templates/most-used")
    @Operation(summary = "Get most used templates",
            description = "Returns a list of templates ordered by usage with optional date filtering")
    public ResponseEntity<MetricsResponse<List<TemplateUsageStats>>> getMostUsedTemplates(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            @Parameter(description = "Start date for filtering (ISO format)") LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            @Parameter(description = "End date for filtering (ISO format)") LocalDateTime endDate,
            @RequestParam(defaultValue = "0") @Parameter(description = "Page number (zero-based)") int page,
            @RequestParam(defaultValue = "10") @Parameter(description = "Page size") int size) {

        log.debug("GET /api/v1/analytics/templates/most-used");

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "totalExecutions"));

        MetricsResponse<List<TemplateUsageStats>> response =
                analyticsService.getMostUsedTemplates(startDate, endDate, pageable);

        return ResponseEntity.ok(response);
    }

    // Version analytics endpoints

    @GetMapping("/versions/{versionId}/performance")
    @Operation(summary = "Get performance statistics for a version",
            description = "Returns detailed performance metrics for the specified prompt version")
    public ResponseEntity<MetricsResponse<VersionPerformanceStats>> getVersionPerformance(
            @PathVariable String versionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            @Parameter(description = "Start date for filtering (ISO format)") LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            @Parameter(description = "End date for filtering (ISO format)") LocalDateTime endDate) {

        log.debug("GET /api/v1/analytics/versions/{}/performance", versionId);

        MetricsResponse<VersionPerformanceStats> response =
                analyticsService.getVersionPerformanceStats(versionId, startDate, endDate);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/versions/compare")
    @Operation(summary = "Compare performance between two versions",
            description = "Returns a comparison of metrics between two prompt versions")
    public ResponseEntity<MetricsResponse<VersionPerformanceStats.VersionComparison>> compareVersions(
            @RequestParam @Parameter(description = "First version ID") String version1,
            @RequestParam @Parameter(description = "Second version ID") String version2) {

        log.debug("GET /api/v1/analytics/versions/compare?version1={}&version2={}", version1, version2);

        MetricsResponse<VersionPerformanceStats.VersionComparison> response =
                analyticsService.compareVersionPerformance(version1, version2);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/templates/{templateId}/versions/performance")
    @Operation(summary = "Get performance statistics for all versions of a template",
            description = "Returns performance metrics for all versions of the specified template")
    public ResponseEntity<MetricsResponse<List<VersionPerformanceStats>>> getVersionsPerformanceForTemplate(
            @PathVariable String templateId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            @Parameter(description = "Start date for filtering (ISO format)") LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            @Parameter(description = "End date for filtering (ISO format)") LocalDateTime endDate) {

        log.debug("GET /api/v1/analytics/templates/{}/versions/performance", templateId);

        MetricsResponse<List<VersionPerformanceStats>> response =
                analyticsService.getVersionsPerformanceForTemplate(templateId, startDate, endDate);

        return ResponseEntity.ok(response);
    }

    // General analytics endpoints

    @GetMapping("/trends/executions")
    @Operation(summary = "Get execution trends over time",
            description = "Returns execution count trends grouped by time interval")
    public ResponseEntity<MetricsResponse<Map<LocalDateTime, Long>>> getExecutionTrends(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            @Parameter(description = "Start date (ISO format)") LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            @Parameter(description = "End date (ISO format)") LocalDateTime endDate,
            @RequestParam(defaultValue = "day")
            @Parameter(description = "Grouping (day, week, month)") String groupBy) {

        log.debug("GET /api/v1/analytics/trends/executions");

        MetricsResponse<Map<LocalDateTime, Long>> response =
                analyticsService.getExecutionTrends(startDate, endDate, groupBy);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/cost/distribution")
    @Operation(summary = "Get cost distribution by provider",
            description = "Returns cost breakdown by provider")
    public ResponseEntity<MetricsResponse<Map<String, BigDecimal>>> getCostDistribution(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            @Parameter(description = "Start date for filtering (ISO format)") LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            @Parameter(description = "End date for filtering (ISO format)") LocalDateTime endDate) {

        log.debug("GET /api/v1/analytics/cost/distribution");

        MetricsResponse<Map<String, BigDecimal>> response =
                analyticsService.getCostDistribution(startDate, endDate);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/tokens/usage")
    @Operation(summary = "Get token usage by model",
            description = "Returns average token usage statistics by model")
    public ResponseEntity<MetricsResponse<Map<String, Double>>> getTokenUsageByModel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            @Parameter(description = "Start date for filtering (ISO format)") LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            @Parameter(description = "End date for filtering (ISO format)") LocalDateTime endDate) {

        log.debug("GET /api/v1/analytics/tokens/usage");

        MetricsResponse<Map<String, Double>> response =
                analyticsService.getTokenUsageByModel(startDate, endDate);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/success-rate/provider")
    @Operation(summary = "Get success rate by provider",
            description = "Returns success rate statistics by provider")
    public ResponseEntity<MetricsResponse<Map<String, Double>>> getSuccessRateByProvider(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            @Parameter(description = "Start date for filtering (ISO format)") LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            @Parameter(description = "End date for filtering (ISO format)") LocalDateTime endDate) {

        log.debug("GET /api/v1/analytics/success-rate/provider");

        MetricsResponse<Map<String, Double>> response =
                analyticsService.getSuccessRateByProvider(startDate, endDate);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Get dashboard summary metrics",
            description = "Returns aggregated metrics for the dashboard")
    public ResponseEntity<MetricsResponse<Map<String, Object>>> getDashboardSummary(
            @RequestParam(defaultValue = "daily")
            @Parameter(description = "Period (daily, weekly, monthly, all_time)") String period) {

        log.debug("GET /api/v1/analytics/dashboard?period={}", period);

        MetricsResponse<Map<String, Object>> response = analyticsService.getDashboardSummary(period);

        return ResponseEntity.ok(response);
    }
}