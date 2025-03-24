package viettel.dac.promptservice.service.analytics;

import org.springframework.data.domain.Pageable;
import viettel.dac.promptservice.dto.analytics.MetricsResponse;
import viettel.dac.promptservice.dto.analytics.TemplateUsageStats;
import viettel.dac.promptservice.dto.analytics.VersionPerformanceStats;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Service for prompt analytics and metrics
 */
public interface PromptAnalyticsService {

    /**
     * Get usage statistics for a specific template
     *
     * @param templateId Template ID
     * @param startDate Start date for the data range (optional)
     * @param endDate End date for the data range (optional)
     * @return Usage statistics wrapped in a metrics response
     */
    MetricsResponse<TemplateUsageStats> getTemplateUsageStats(
            String templateId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get usage statistics for multiple templates
     *
     * @param templateIds List of template IDs
     * @param startDate Start date for the data range (optional)
     * @param endDate End date for the data range (optional)
     * @return Map of template ID to usage statistics wrapped in a metrics response
     */
    MetricsResponse<Map<String, TemplateUsageStats>> getMultipleTemplateUsageStats(
            List<String> templateIds, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get most used templates
     *
     * @param startDate Start date for the data range (optional)
     * @param endDate End date for the data range (optional)
     * @param pageable Pagination information
     * @return List of template usage statistics wrapped in a metrics response
     */
    MetricsResponse<List<TemplateUsageStats>> getMostUsedTemplates(
            LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    /**
     * Get performance statistics for a specific version
     *
     * @param versionId Version ID
     * @param startDate Start date for the data range (optional)
     * @param endDate End date for the data range (optional)
     * @return Version performance statistics wrapped in a metrics response
     */
    MetricsResponse<VersionPerformanceStats> getVersionPerformanceStats(
            String versionId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Compare performance between two versions
     *
     * @param versionId1 First version ID
     * @param versionId2 Second version ID
     * @return Version comparison wrapped in a metrics response
     */
    MetricsResponse<VersionPerformanceStats.VersionComparison> compareVersionPerformance(
            String versionId1, String versionId2);

    /**
     * Get performance statistics for all versions of a template
     *
     * @param templateId Template ID
     * @param startDate Start date for the data range (optional)
     * @param endDate End date for the data range (optional)
     * @return List of version performance statistics wrapped in a metrics response
     */
    MetricsResponse<List<VersionPerformanceStats>> getVersionsPerformanceForTemplate(
            String templateId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get execution trends over time
     *
     * @param startDate Start date for the data range
     * @param endDate End date for the data range
     * @param groupBy Grouping interval (day, week, month)
     * @return Time series data wrapped in a metrics response
     */
    MetricsResponse<Map<LocalDateTime, Long>> getExecutionTrends(
            LocalDateTime startDate, LocalDateTime endDate, String groupBy);

    /**
     * Get cost distribution by provider
     *
     * @param startDate Start date for the data range (optional)
     * @param endDate End date for the data range (optional)
     * @return Cost distribution wrapped in a metrics response
     */
    MetricsResponse<Map<String, BigDecimal>> getCostDistribution(
            LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get token usage by model
     *
     * @param startDate Start date for the data range (optional)
     * @param endDate End date for the data range (optional)
     * @return Token usage wrapped in a metrics response
     */
    MetricsResponse<Map<String, Double>> getTokenUsageByModel(
            LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get success rate by provider
     *
     * @param startDate Start date for the data range (optional)
     * @param endDate End date for the data range (optional)
     * @return Success rate wrapped in a metrics response
     */
    MetricsResponse<Map<String, Double>> getSuccessRateByProvider(
            LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get dashboard summary metrics
     *
     * @param period Period for which metrics are calculated (daily, weekly, monthly, all)
     * @return Dashboard summary wrapped in a metrics response
     */
    MetricsResponse<Map<String, Object>> getDashboardSummary(String period);
}