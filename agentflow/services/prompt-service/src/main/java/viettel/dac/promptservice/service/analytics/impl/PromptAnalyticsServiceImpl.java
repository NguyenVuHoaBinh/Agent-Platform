package viettel.dac.promptservice.service.analytics.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import viettel.dac.promptservice.dto.analytics.MetricsResponse;
import viettel.dac.promptservice.dto.analytics.TemplateUsageStats;
import viettel.dac.promptservice.dto.analytics.VersionPerformanceStats;
import viettel.dac.promptservice.model.entity.PromptExecution;
import viettel.dac.promptservice.model.enums.ExecutionStatus;
import viettel.dac.promptservice.repository.analytics.AnalyticsRepository;
import viettel.dac.promptservice.repository.jpa.PromptExecutionRepository;
import viettel.dac.promptservice.repository.jpa.PromptTemplateRepository;
import viettel.dac.promptservice.service.analytics.PromptAnalyticsService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of the analytics service
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PromptAnalyticsServiceImpl implements PromptAnalyticsService {

    private final AnalyticsRepository analyticsRepository;
    private final PromptExecutionRepository executionRepository;
    private final PromptTemplateRepository templateRepository;

    // Time period constants
    private static final String DAILY = "daily";
    private static final String WEEKLY = "weekly";
    private static final String MONTHLY = "monthly";
    private static final String ALL_TIME = "all_time";

    @Override
    public MetricsResponse<TemplateUsageStats> getTemplateUsageStats(
            String templateId, LocalDateTime startDate, LocalDateTime endDate) {

        log.debug("Getting template usage stats for template: {}", templateId);

        TemplateUsageStats stats = analyticsRepository.getTemplateUsageStats(templateId, startDate, endDate);

        String period = getPeriodString(startDate, endDate);

        return MetricsResponse.<TemplateUsageStats>builder()
                .metricType("template_usage")
                .period(period)
                .startTime(startDate)
                .endTime(endDate)
                .data(stats)
                .metadata(Map.of("templateId", templateId))
                .build();
    }

    @Override
    public MetricsResponse<Map<String, TemplateUsageStats>> getMultipleTemplateUsageStats(
            List<String> templateIds, LocalDateTime startDate, LocalDateTime endDate) {

        log.debug("Getting template usage stats for {} templates", templateIds.size());

        Map<String, TemplateUsageStats> stats = analyticsRepository.getTemplateUsageStats(templateIds, startDate, endDate);

        String period = getPeriodString(startDate, endDate);

        return MetricsResponse.<Map<String, TemplateUsageStats>>builder()
                .metricType("multiple_template_usage")
                .period(period)
                .startTime(startDate)
                .endTime(endDate)
                .data(stats)
                .metadata(Map.of("templateCount", templateIds.size()))
                .build();
    }

    @Override
    public MetricsResponse<List<TemplateUsageStats>> getMostUsedTemplates(
            LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {

        log.debug("Getting most used templates");

        List<TemplateUsageStats> stats = analyticsRepository.getMostUsedTemplates(startDate, endDate, pageable);

        String period = getPeriodString(startDate, endDate);

        return MetricsResponse.<List<TemplateUsageStats>>builder()
                .metricType("most_used_templates")
                .period(period)
                .startTime(startDate)
                .endTime(endDate)
                .data(stats)
                .metadata(Map.of(
                        "page", pageable.getPageNumber(),
                        "size", pageable.getPageSize(),
                        "resultCount", stats.size()))
                .build();
    }

    @Override
    public MetricsResponse<VersionPerformanceStats> getVersionPerformanceStats(
            String versionId, LocalDateTime startDate, LocalDateTime endDate) {

        log.debug("Getting version performance stats for version: {}", versionId);

        VersionPerformanceStats stats = analyticsRepository.getVersionPerformanceStats(versionId, startDate, endDate);

        String period = getPeriodString(startDate, endDate);

        return MetricsResponse.<VersionPerformanceStats>builder()
                .metricType("version_performance")
                .period(period)
                .startTime(startDate)
                .endTime(endDate)
                .data(stats)
                .metadata(Map.of(
                        "versionId", versionId,
                        "templateId", stats.getTemplateId()))
                .build();
    }

    @Override
    public MetricsResponse<VersionPerformanceStats.VersionComparison> compareVersionPerformance(
            String versionId1, String versionId2) {

        log.debug("Comparing versions: {} and {}", versionId1, versionId2);

        VersionPerformanceStats.VersionComparison comparison =
                analyticsRepository.compareVersionPerformance(versionId1, versionId2);

        return MetricsResponse.<VersionPerformanceStats.VersionComparison>builder()
                .metricType("version_comparison")
                .period(ALL_TIME)
                .data(comparison)
                .metadata(Map.of(
                        "versionId1", versionId1,
                        "versionId2", versionId2))
                .build();
    }

    @Override
    public MetricsResponse<List<VersionPerformanceStats>> getVersionsPerformanceForTemplate(
            String templateId, LocalDateTime startDate, LocalDateTime endDate) {

        log.debug("Getting performance stats for all versions of template: {}", templateId);

        List<VersionPerformanceStats> stats =
                analyticsRepository.getVersionsPerformanceForTemplate(templateId, startDate, endDate);

        String period = getPeriodString(startDate, endDate);

        return MetricsResponse.<List<VersionPerformanceStats>>builder()
                .metricType("template_versions_performance")
                .period(period)
                .startTime(startDate)
                .endTime(endDate)
                .data(stats)
                .metadata(Map.of(
                        "templateId", templateId,
                        "versionCount", stats.size()))
                .build();
    }

    @Override
    public MetricsResponse<Map<LocalDateTime, Long>> getExecutionTrends(
            LocalDateTime startDate, LocalDateTime endDate, String groupBy) {

        log.debug("Getting execution trends from {} to {} grouped by {}", startDate, endDate, groupBy);

        Map<LocalDateTime, Long> trends = analyticsRepository.getExecutionCountByDate(startDate, endDate, groupBy);

        // Calculate total executions and average per period
        long totalExecutions = trends.values().stream().mapToLong(Long::longValue).sum();
        double averagePerPeriod = trends.isEmpty() ? 0 : (double) totalExecutions / trends.size();

        String period = getPeriodFromGroupBy(groupBy);

        return MetricsResponse.<Map<LocalDateTime, Long>>builder()
                .metricType("execution_trends")
                .period(period)
                .startTime(startDate)
                .endTime(endDate)
                .data(trends)
                .metadata(Map.of(
                        "groupBy", groupBy,
                        "totalExecutions", totalExecutions,
                        "averagePerPeriod", Math.round(averagePerPeriod * 100) / 100.0))
                .build();
    }

    @Override
    public MetricsResponse<Map<String, BigDecimal>> getCostDistribution(
            LocalDateTime startDate, LocalDateTime endDate) {

        log.debug("Getting cost distribution from {} to {}", startDate, endDate);

        Map<String, BigDecimal> costDistribution = analyticsRepository.getCostByProvider(startDate, endDate);

        // Calculate total cost
        BigDecimal totalCost = costDistribution.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String period = getPeriodString(startDate, endDate);

        return MetricsResponse.<Map<String, BigDecimal>>builder()
                .metricType("cost_distribution")
                .period(period)
                .startTime(startDate)
                .endTime(endDate)
                .data(costDistribution)
                .metadata(Map.of(
                        "totalCost", totalCost,
                        "providerCount", costDistribution.size()))
                .build();
    }

    @Override
    public MetricsResponse<Map<String, Double>> getTokenUsageByModel(
            LocalDateTime startDate, LocalDateTime endDate) {

        log.debug("Getting token usage by model from {} to {}", startDate, endDate);

        Map<String, Double> tokenUsage = analyticsRepository.getAverageTokensByModel(startDate, endDate);

        String period = getPeriodString(startDate, endDate);

        return MetricsResponse.<Map<String, Double>>builder()
                .metricType("token_usage")
                .period(period)
                .startTime(startDate)
                .endTime(endDate)
                .data(tokenUsage)
                .metadata(Map.of("modelCount", tokenUsage.size()))
                .build();
    }

    @Override
    public MetricsResponse<Map<String, Double>> getSuccessRateByProvider(
            LocalDateTime startDate, LocalDateTime endDate) {

        log.debug("Getting success rate by provider from {} to {}", startDate, endDate);

        Map<String, Double> successRates = analyticsRepository.getSuccessRateByProvider(startDate, endDate);

        String period = getPeriodString(startDate, endDate);

        return MetricsResponse.<Map<String, Double>>builder()
                .metricType("success_rate")
                .period(period)
                .startTime(startDate)
                .endTime(endDate)
                .data(successRates)
                .metadata(Map.of("providerCount", successRates.size()))
                .build();
    }

    @Override
    public MetricsResponse<Map<String, Object>> getDashboardSummary(String period) {
        log.debug("Getting dashboard summary for period: {}", period);

        // Define date range based on period
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = getStartDateFromPeriod(endDate, period);

        // Fetch various metrics for the dashboard
        Map<String, Object> dashboardData = new HashMap<>();

        // Get total execution stats
        long totalExecutions = executionRepository.count();
        long successfulExecutions = executionRepository.countByStatus(ExecutionStatus.SUCCESS);
        double successRate = totalExecutions > 0 ? (double) successfulExecutions / totalExecutions * 100 : 0;

        dashboardData.put("totalExecutions", totalExecutions);
        dashboardData.put("successfulExecutions", successfulExecutions);
        dashboardData.put("successRate", Math.round(successRate * 100) / 100.0);

        // Get template stats
        long totalTemplates = templateRepository.count();
        long templatesWithPublishedVersions = templateRepository.countWithPublishedVersions();

        dashboardData.put("totalTemplates", totalTemplates);
        dashboardData.put("templatesWithPublishedVersions", templatesWithPublishedVersions);

        // Get execution trends for the period
        String groupBy = getGroupByFromPeriod(period);
        Map<LocalDateTime, Long> executionTrends = analyticsRepository.getExecutionCountByDate(startDate, endDate, groupBy);

        dashboardData.put("executionTrends", executionTrends);

        // Get cost data
        Map<String, BigDecimal> costByProvider = analyticsRepository.getCostByProvider(startDate, endDate);
        BigDecimal totalCost = costByProvider.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        dashboardData.put("totalCost", totalCost);
        dashboardData.put("costByProvider", costByProvider);

        // Get most used templates (top 5)
        List<TemplateUsageStats> mostUsedTemplates = analyticsRepository.getMostUsedTemplates(
                startDate, endDate, Pageable.ofSize(5));

        dashboardData.put("mostUsedTemplates", mostUsedTemplates);

        return MetricsResponse.<Map<String, Object>>builder()
                .metricType("dashboard_summary")
                .period(period)
                .startTime(startDate)
                .endTime(endDate)
                .data(dashboardData)
                .build();
    }

    /**
     * Get a string representation of the date range period
     */
    private String getPeriodString(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null || endDate == null) {
            return ALL_TIME;
        }

        // Default to custom range
        String period = "custom";

        // Check for common periods
        LocalDateTime now = LocalDateTime.now();

        if (startDate.truncatedTo(ChronoUnit.DAYS).equals(now.truncatedTo(ChronoUnit.DAYS).minusDays(1))
                && endDate.truncatedTo(ChronoUnit.DAYS).equals(now.truncatedTo(ChronoUnit.DAYS))) {
            period = DAILY;
        } else if (startDate.truncatedTo(ChronoUnit.DAYS).equals(now.truncatedTo(ChronoUnit.DAYS).minusWeeks(1))
                && endDate.truncatedTo(ChronoUnit.DAYS).equals(now.truncatedTo(ChronoUnit.DAYS))) {
            period = WEEKLY;
        } else if (startDate.truncatedTo(ChronoUnit.DAYS).equals(now.truncatedTo(ChronoUnit.DAYS).minusMonths(1))
                && endDate.truncatedTo(ChronoUnit.DAYS).equals(now.truncatedTo(ChronoUnit.DAYS))) {
            period = MONTHLY;
        }

        return period;
    }

    /**
     * Get start date based on period string
     */
    private LocalDateTime getStartDateFromPeriod(LocalDateTime endDate, String period) {
        switch (period.toLowerCase()) {
            case DAILY:
                return endDate.minusDays(1).truncatedTo(ChronoUnit.DAYS);
            case WEEKLY:
                return endDate.minusWeeks(1).truncatedTo(ChronoUnit.DAYS);
            case MONTHLY:
                return endDate.minusMonths(1).truncatedTo(ChronoUnit.DAYS);
            case ALL_TIME:
            default:
                return null;
        }
    }

    /**
     * Get appropriate groupBy value based on period
     */
    private String getGroupByFromPeriod(String period) {
        switch (period.toLowerCase()) {
            case DAILY:
                return "hour";
            case WEEKLY:
                return "day";
            case MONTHLY:
            case ALL_TIME:
            default:
                return "day";
        }
    }

    /**
     * Get period string from groupBy value
     */
    private String getPeriodFromGroupBy(String groupBy) {
        switch (groupBy.toLowerCase()) {
            case "hour":
                return DAILY;
            case "day":
                return WEEKLY;
            case "week":
                return MONTHLY;
            case "month":
                return "yearly";
            default:
                return "custom";
        }
    }
}