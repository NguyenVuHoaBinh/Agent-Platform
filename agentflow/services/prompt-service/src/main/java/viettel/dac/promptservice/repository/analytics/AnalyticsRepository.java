package viettel.dac.promptservice.repository.analytics;

import org.springframework.data.domain.Pageable;
import viettel.dac.promptservice.dto.analytics.TemplateUsageStats;
import viettel.dac.promptservice.dto.analytics.VersionPerformanceStats;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Repository for analytics data, providing aggregated metrics
 */
public interface AnalyticsRepository {

    /**
     * Get usage statistics for a specific template
     *
     * @param templateId Template ID
     * @param startDate Start date for the data range (optional)
     * @param endDate End date for the data range (optional)
     * @return Template usage statistics
     */
    TemplateUsageStats getTemplateUsageStats(String templateId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get usage statistics for multiple templates
     *
     * @param templateIds List of template IDs
     * @param startDate Start date for the data range (optional)
     * @param endDate End date for the data range (optional)
     * @return Map of template ID to usage statistics
     */
    Map<String, TemplateUsageStats> getTemplateUsageStats(List<String> templateIds, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get most used templates
     *
     * @param startDate Start date for the data range (optional)
     * @param endDate End date for the data range (optional)
     * @param pageable Pagination information
     * @return List of template usage statistics, ordered by usage
     */
    List<TemplateUsageStats> getMostUsedTemplates(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    /**
     * Get performance statistics for a specific version
     *
     * @param versionId Version ID
     * @param startDate Start date for the data range (optional)
     * @param endDate End date for the data range (optional)
     * @return Version performance statistics
     */
    VersionPerformanceStats getVersionPerformanceStats(String versionId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get performance comparison between two versions
     *
     * @param versionId1 First version ID
     * @param versionId2 Second version ID
     * @return Comparison of performance statistics
     */
    VersionPerformanceStats.VersionComparison compareVersionPerformance(String versionId1, String versionId2);

    /**
     * Get performance statistics for all versions of a template
     *
     * @param templateId Template ID
     * @param startDate Start date for the data range (optional)
     * @param endDate End date for the data range (optional)
     * @return List of version performance statistics
     */
    List<VersionPerformanceStats> getVersionsPerformanceForTemplate(String templateId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get execution count by date
     *
     * @param startDate Start date for the data range
     * @param endDate End date for the data range
     * @param groupBy Grouping interval (day, week, month)
     * @return Map of date to execution count
     */
    Map<LocalDateTime, Long> getExecutionCountByDate(LocalDateTime startDate, LocalDateTime endDate, String groupBy);

    /**
     * Get cost by provider
     *
     * @param startDate Start date for the data range (optional)
     * @param endDate End date for the data range (optional)
     * @return Map of provider ID to total cost
     */
    Map<String, BigDecimal> getCostByProvider(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get average tokens by model
     *
     * @param startDate Start date for the data range (optional)
     * @param endDate End date for the data range (optional)
     * @return Map of model ID to average token count
     */
    Map<String, Double> getAverageTokensByModel(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get success rate by provider
     *
     * @param startDate Start date for the data range (optional)
     * @param endDate End date for the data range (optional)
     * @return Map of provider ID to success rate
     */
    Map<String, Double> getSuccessRateByProvider(LocalDateTime startDate, LocalDateTime endDate);
}