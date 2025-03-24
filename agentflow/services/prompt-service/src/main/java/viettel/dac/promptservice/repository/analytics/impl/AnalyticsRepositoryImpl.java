package viettel.dac.promptservice.repository.analytics.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import viettel.dac.promptservice.dto.analytics.TemplateUsageStats;
import viettel.dac.promptservice.dto.analytics.VersionPerformanceStats;
import viettel.dac.promptservice.model.entity.PromptExecution;
import viettel.dac.promptservice.model.entity.PromptTemplate;
import viettel.dac.promptservice.model.entity.PromptVersion;
import viettel.dac.promptservice.model.enums.ExecutionStatus;
import viettel.dac.promptservice.repository.analytics.AnalyticsRepository;
import viettel.dac.promptservice.repository.jpa.PromptExecutionRepository;
import viettel.dac.promptservice.repository.jpa.PromptTemplateRepository;
import viettel.dac.promptservice.repository.jpa.PromptVersionRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Implementation of analytics repository for aggregating and retrieving metrics
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class AnalyticsRepositoryImpl implements AnalyticsRepository {

    private final PromptExecutionRepository executionRepository;
    private final PromptVersionRepository versionRepository;
    private final PromptTemplateRepository templateRepository;

    private static final String DAY = "day";
    private static final String WEEK = "week";
    private static final String MONTH = "month";

    /**
     * Rounds a double value to 2 decimal places
     */
    private double round(double value) {
        return Math.round(value * 100) / 100.0;
    }

    @Override
    public TemplateUsageStats getTemplateUsageStats(String templateId, LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Getting usage stats for template: {}", templateId);

        if (!templateRepository.existsById(templateId)) {
            throw new IllegalArgumentException("Template not found with id: " + templateId);
        }

        PromptTemplate template = templateRepository.findById(templateId).orElseThrow();

        // Get all versions for this template
        List<PromptVersion> versions = versionRepository.findByTemplateId(templateId);
        List<String> versionIds = versions.stream()
                .map(PromptVersion::getId)
                .collect(Collectors.toList());

        // Start building the response
        TemplateUsageStats.TemplateUsageStatsBuilder statsBuilder = TemplateUsageStats.builder()
                .templateId(templateId)
                .templateName(template.getName());

        long totalExecutions = 0;
        long successfulExecutions = 0;
        long totalTokens = 0;
        BigDecimal totalCost = BigDecimal.ZERO;
        double totalResponseTime = 0;
        LocalDateTime firstExecution = null;
        LocalDateTime lastExecution = null;

        Map<String, Long> executionsByModel = new HashMap<>();
        Map<String, Long> executionsByProvider = new HashMap<>();

        // Get and process executions for each version
        for (String versionId : versionIds) {
            List<PromptExecution> executions = getVersionExecutions(versionId, startDate, endDate);

            // Track statistics
            totalExecutions += executions.size();

            for (PromptExecution execution : executions) {
                // Track first and last execution
                if (firstExecution == null || execution.getExecutedAt().isBefore(firstExecution)) {
                    firstExecution = execution.getExecutedAt();
                }
                if (lastExecution == null || execution.getExecutedAt().isAfter(lastExecution)) {
                    lastExecution = execution.getExecutedAt();
                }

                // Track success/failure
                if (execution.isSuccessful()) {
                    successfulExecutions++;
                }

                // Track tokens
                if (execution.getTokenCount() != null) {
                    totalTokens += execution.getTokenCount();
                }

                // Track cost
                if (execution.getCost() != null) {
                    totalCost = totalCost.add(execution.getCost());
                }

                // Track response time
                if (execution.getResponseTimeMs() != null) {
                    totalResponseTime += execution.getResponseTimeMs();
                }

                // Track by model
                executionsByModel.compute(execution.getModelId(), (k, v) -> (v == null) ? 1L : v + 1);

                // Track by provider
                executionsByProvider.compute(execution.getProviderId(), (k, v) -> (v == null) ? 1L : v + 1);
            }
        }

        // Calculate averages and rates
        double successRate = totalExecutions > 0
                ? (double) successfulExecutions / totalExecutions * 100
                : 0;

        double averageResponseTime = totalExecutions > 0
                ? totalResponseTime / totalExecutions
                : 0;

        // Build time series data if date range is specified
        List<TemplateUsageStats.TimeSeriesDataPoint> timeSeriesData = null;
        if (startDate != null && endDate != null) {
            timeSeriesData = buildTimeSeriesData(templateId, startDate, endDate);
        }

        // Complete building the response
        return statsBuilder
                .totalExecutions(totalExecutions)
                .successfulExecutions(successfulExecutions)
                .failedExecutions(totalExecutions - successfulExecutions)
                .successRate(round(successRate))
                .totalTokens(totalTokens)
                .totalCost(totalCost)
                .averageResponseTime(round(averageResponseTime))
                .executionsByModel(executionsByModel)
                .executionsByProvider(executionsByProvider)
                .executionsOverTime(timeSeriesData)
                .firstExecution(firstExecution)
                .lastExecution(lastExecution)
                .build();
    }

    /**
     * Get executions for a version within the specified date range
     */
    private List<PromptExecution> getVersionExecutions(String versionId, LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate != null && endDate != null) {
            return executionRepository.findByVersionIdAndDateRange(versionId, startDate, endDate, Pageable.unpaged())
                    .getContent();
        } else {
            return executionRepository.findByVersionId(versionId);
        }
    }

    /**
     * Build time series data for executions over time
     */
    private List<TemplateUsageStats.TimeSeriesDataPoint> buildTimeSeriesData(
            String templateId, LocalDateTime startDate, LocalDateTime endDate) {

        try {
            List<Object[]> executionsPerDay = executionRepository.getExecutionsPerDay(templateId);
            Map<String, Long> executionCountByDay = new HashMap<>();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            // Convert raw data to map
            for (Object[] row : executionsPerDay) {
                String day = row[0].toString();
                Long count = Long.valueOf(row[1].toString());
                executionCountByDay.put(day, count);
            }

            // Create time series with all days in range
            List<TemplateUsageStats.TimeSeriesDataPoint> timeSeriesData = new ArrayList<>();
            LocalDateTime current = startDate;

            while (!current.isAfter(endDate)) {
                String key = current.format(formatter);
                Long count = executionCountByDay.getOrDefault(key, 0L);

                timeSeriesData.add(TemplateUsageStats.TimeSeriesDataPoint.builder()
                        .timestamp(current)
                        .value(count)
                        .build());

                current = current.plusDays(1);
            }

            return timeSeriesData;
        } catch (Exception e) {
            log.error("Error building time series data", e);
            return Collections.emptyList();
        }
    }

    @Override
    public Map<String, TemplateUsageStats> getTemplateUsageStats(
            List<String> templateIds, LocalDateTime startDate, LocalDateTime endDate) {

        return templateIds.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        id -> getTemplateUsageStats(id, startDate, endDate)));
    }

    @Override
    public List<TemplateUsageStats> getMostUsedTemplates(
            LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {

        // Get templates ordered by usage
        List<Object[]> templateUsageCounts;

        if (startDate != null && endDate != null) {
            // Get counts within date range
            templateUsageCounts = executionRepository.getExecutionCountByStatus(startDate, endDate);
        } else {
            // Get all-time counts
            List<PromptTemplate> allTemplates = templateRepository.findAll();
            templateUsageCounts = new ArrayList<>();

            for (PromptTemplate template : allTemplates) {
                long count = executionRepository.countByTemplateIdAndStatus(
                        template.getId(), ExecutionStatus.SUCCESS);

                if (count > 0) {
                    templateUsageCounts.add(new Object[]{template.getId(), count});
                }
            }
        }

        // Sort by count descending
        templateUsageCounts.sort((a, b) -> Long.compare((Long)b[1], (Long)a[1]));

        // Apply pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), templateUsageCounts.size());

        List<Object[]> pagedResults = start < end
                ? templateUsageCounts.subList(start, end)
                : Collections.emptyList();

        // Convert to stats objects
        return pagedResults.stream()
                .map(row -> {
                    String templateId = (String) row[0];
                    return getTemplateUsageStats(templateId, startDate, endDate);
                })
                .collect(Collectors.toList());
    }

    @Override
    public VersionPerformanceStats getVersionPerformanceStats(
            String versionId, LocalDateTime startDate, LocalDateTime endDate) {

        log.debug("Getting performance stats for version: {}", versionId);

        if (!versionRepository.existsById(versionId)) {
            throw new IllegalArgumentException("Version not found with id: " + versionId);
        }

        // Get the version
        PromptVersion version = versionRepository.findById(versionId).orElseThrow();
        PromptTemplate template = version.getTemplate();

        // Get executions for this version
        List<PromptExecution> executions = getVersionExecutions(versionId, startDate, endDate);

        // Track statistics
        long totalExecutions = executions.size();
        long successfulExecutions = 0;
        long totalTokens = 0;
        long totalInputTokens = 0;
        long totalOutputTokens = 0;
        double totalResponseTime = 0;
        BigDecimal totalCost = BigDecimal.ZERO;

        // Track model-specific data
        Map<String, Map<String, Object>> modelStats = new HashMap<>();

        for (PromptExecution execution : executions) {
            // Track success/failure
            if (execution.isSuccessful()) {
                successfulExecutions++;
            }

            // Track tokens
            if (execution.getTokenCount() != null) {
                totalTokens += execution.getTokenCount();
            }

            if (execution.getInputTokens() != null) {
                totalInputTokens += execution.getInputTokens();
            }

            if (execution.getOutputTokens() != null) {
                totalOutputTokens += execution.getOutputTokens();
            }

            // Track cost
            if (execution.getCost() != null) {
                totalCost = totalCost.add(execution.getCost());
            }

            // Track response time
            if (execution.getResponseTimeMs() != null) {
                totalResponseTime += execution.getResponseTimeMs();
            }

            // Track by model
            String key = execution.getModelId() + "::" + execution.getProviderId();
            modelStats.computeIfAbsent(key, k -> {
                Map<String, Object> stats = new HashMap<>();
                stats.put("modelId", execution.getModelId());
                stats.put("providerId", execution.getProviderId());
                stats.put("executions", 0L);
                stats.put("successfulExecutions", 0L);
                stats.put("totalTokens", 0L);
                stats.put("totalResponseTime", 0.0);
                stats.put("totalCost", BigDecimal.ZERO);
                return stats;
            });

            Map<String, Object> stats = modelStats.get(key);
            stats.put("executions", (Long)stats.get("executions") + 1);

            if (execution.isSuccessful()) {
                stats.put("successfulExecutions", (Long)stats.get("successfulExecutions") + 1);
            }

            if (execution.getTokenCount() != null) {
                stats.put("totalTokens", (Long)stats.get("totalTokens") + execution.getTokenCount());
            }

            if (execution.getResponseTimeMs() != null) {
                stats.put("totalResponseTime", (Double)stats.get("totalResponseTime") + execution.getResponseTimeMs());
            }

            if (execution.getCost() != null) {
                stats.put("totalCost", ((BigDecimal)stats.get("totalCost")).add(execution.getCost()));
            }
        }

        // Calculate averages and rates
        double successRate = totalExecutions > 0
                ? (double) successfulExecutions / totalExecutions * 100
                : 0;

        double averageTokens = totalExecutions > 0
                ? (double) totalTokens / totalExecutions
                : 0;

        double averageInputTokens = totalExecutions > 0
                ? (double) totalInputTokens / totalExecutions
                : 0;

        double averageOutputTokens = totalExecutions > 0
                ? (double) totalOutputTokens / totalExecutions
                : 0;

        double averageResponseTime = totalExecutions > 0
                ? totalResponseTime / totalExecutions
                : 0;

        BigDecimal averageCost = totalExecutions > 0
                ? totalCost.divide(BigDecimal.valueOf(totalExecutions), 6, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Convert model stats to response format
        List<VersionPerformanceStats.ModelPerformance> performanceByModel = modelStats.values().stream()
                .map(stats -> {
                    long modelExecutions = (Long) stats.get("executions");
                    long modelSuccessful = (Long) stats.get("successfulExecutions");
                    double modelSuccessRate = modelExecutions > 0
                            ? (double) modelSuccessful / modelExecutions * 100
                            : 0;

                    long modelTokens = (Long) stats.get("totalTokens");
                    double modelAverageTokens = modelExecutions > 0
                            ? (double) modelTokens / modelExecutions
                            : 0;

                    double modelResponseTime = (Double) stats.get("totalResponseTime");
                    double modelAverageResponseTime = modelExecutions > 0
                            ? modelResponseTime / modelExecutions
                            : 0;

                    BigDecimal modelCost = (BigDecimal) stats.get("totalCost");
                    BigDecimal modelAverageCost = modelExecutions > 0
                            ? modelCost.divide(BigDecimal.valueOf(modelExecutions), 6, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;

                    return VersionPerformanceStats.ModelPerformance.builder()
                            .modelId((String) stats.get("modelId"))
                            .providerId((String) stats.get("providerId"))
                            .executions(modelExecutions)
                            .successRate(round(modelSuccessRate))
                            .averageTokens(round(modelAverageTokens))
                            .averageResponseTime(round(modelAverageResponseTime))
                            .averageCost(modelAverageCost)
                            .build();
                })
                .collect(Collectors.toList());

        // Get comparison with previous version
        VersionPerformanceStats.VersionComparison comparison = null;
        if (version.getParentVersion() != null) {
            comparison = compareVersionPerformance(version.getParentVersion().getId(), versionId);
        }

        // Build response
        return VersionPerformanceStats.builder()
                .versionId(versionId)
                .versionNumber(version.getVersionNumber())
                .templateId(template.getId())
                .templateName(template.getName())
                .totalExecutions(totalExecutions)
                .successRate(round(successRate))
                .averageTokens(round(averageTokens))
                .averageInputTokens(round(averageInputTokens))
                .averageOutputTokens(round(averageOutputTokens))
                .averageResponseTime(round(averageResponseTime))
                .averageCost(averageCost)
                .totalCost(totalCost)
                .performanceByModel(performanceByModel)
                .comparisonWithPrevious(comparison)
                .build();
    }

    @Override
    public VersionPerformanceStats.VersionComparison compareVersionPerformance(String versionId1, String versionId2) {
        log.debug("Comparing performance of versions: {} and {}", versionId1, versionId2);

        if (!versionRepository.existsById(versionId1)) {
            throw new IllegalArgumentException("Version not found with id: " + versionId1);
        }

        if (!versionRepository.existsById(versionId2)) {
            throw new IllegalArgumentException("Version not found with id: " + versionId2);
        }

        // Get the versions
        PromptVersion version1 = versionRepository.findById(versionId1).orElseThrow();
        PromptVersion version2 = versionRepository.findById(versionId2).orElseThrow();

        // Calculate metrics for each version
        Double successRate1 = executionRepository.getSuccessRate(versionId1);
        Double successRate2 = executionRepository.getSuccessRate(versionId2);

        Double avgTokens1 = executionRepository.getAverageTokenCount(versionId1);
        Double avgTokens2 = executionRepository.getAverageTokenCount(versionId2);

        Long avgResponseTime1 = executionRepository.getAverageResponseTime(versionId1);
        Long avgResponseTime2 = executionRepository.getAverageResponseTime(versionId2);

        Double avgCost1 = executionRepository.getAverageCost(versionId1);
        Double avgCost2 = executionRepository.getAverageCost(versionId2);

        // Calculate differences
        Double tokensDiff = calculatePercentageDiff(avgTokens1, avgTokens2);
        Double responseTimeDiff = calculatePercentageDiff(
                avgResponseTime1 != null ? avgResponseTime1.doubleValue() : null,
                avgResponseTime2 != null ? avgResponseTime2.doubleValue() : null);
        Double costDiff = calculatePercentageDiff(avgCost1, avgCost2);

        Double successRateDiff = (successRate2 != null && successRate1 != null)
                ? successRate2 - successRate1
                : null;

        // Build response
        return VersionPerformanceStats.VersionComparison.builder()
                .previousVersionId(versionId1)
                .previousVersionNumber(version1.getVersionNumber())
                .tokensDiff(tokensDiff != null ? round(tokensDiff) : null)
                .responseTimeDiff(responseTimeDiff != null ? round(responseTimeDiff) : null)
                .costDiff(costDiff != null ? round(costDiff) : null)
                .successRateDiff(successRateDiff != null ? round(successRateDiff) : null)
                .build();
    }

    /**
     * Calculate percentage difference between two values
     */
    private Double calculatePercentageDiff(Double value1, Double value2) {
        if (value1 == null || value2 == null || value1 == 0) {
            return null;
        }
        return ((value2 - value1) / value1) * 100;
    }

    @Override
    public List<VersionPerformanceStats> getVersionsPerformanceForTemplate(
            String templateId, LocalDateTime startDate, LocalDateTime endDate) {

        log.debug("Getting performance stats for all versions of template: {}", templateId);

        if (!templateRepository.existsById(templateId)) {
            throw new IllegalArgumentException("Template not found with id: " + templateId);
        }

        // Get all versions for this template
        List<PromptVersion> versions = versionRepository.findByTemplateId(templateId);

        // Get stats for each version
        return versions.stream()
                .map(version -> getVersionPerformanceStats(version.getId(), startDate, endDate))
                .collect(Collectors.toList());
    }

    @Override
    public Map<LocalDateTime, Long> getExecutionCountByDate(
            LocalDateTime startDate, LocalDateTime endDate, String groupBy) {

        log.debug("Getting execution count by date from {} to {} grouped by {}", startDate, endDate, groupBy);

        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and end date are required");
        }

        TemporalUnit unit;
        if (MONTH.equalsIgnoreCase(groupBy)) {
            unit = ChronoUnit.MONTHS;
        } else if (WEEK.equalsIgnoreCase(groupBy)) {
            unit = ChronoUnit.WEEKS;
        } else {
            unit = ChronoUnit.DAYS;
        }

        // Get execution counts by status from the repository
        List<Object[]> executionCounts = executionRepository.getExecutionCountByStatus(startDate, endDate);

        // Map to hold aggregated data
        Map<LocalDateTime, Long> countByDate = new TreeMap<>();

        // Initialize map with all periods
        LocalDateTime current = startDate;
        while (!current.isAfter(endDate)) {
            countByDate.put(current, 0L);

            if (MONTH.equalsIgnoreCase(groupBy)) {
                current = current.plusMonths(1);
            } else if (WEEK.equalsIgnoreCase(groupBy)) {
                current = current.plusWeeks(1);
            } else {
                current = current.plusDays(1);
            }
        }

        // Fill in actual data
        for (Object[] row : executionCounts) {
            ExecutionStatus status = ExecutionStatus.valueOf(row[0].toString());
            long count = Long.parseLong(row[1].toString());

            if (status == ExecutionStatus.SUCCESS) {
                LocalDateTime date = (LocalDateTime) row[2];

                // Truncate the date based on groupBy
                LocalDateTime truncatedDate;
                if (MONTH.equalsIgnoreCase(groupBy)) {
                    truncatedDate = date.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
                } else if (WEEK.equalsIgnoreCase(groupBy)) {
                    truncatedDate = date.minusDays(date.getDayOfWeek().getValue() - 1)
                            .withHour(0).withMinute(0).withSecond(0).withNano(0);
                } else {
                    truncatedDate = date.withHour(0).withMinute(0).withSecond(0).withNano(0);
                }

                countByDate.put(truncatedDate, countByDate.getOrDefault(truncatedDate, 0L) + count);
            }
        }

        return countByDate;
    }

    @Override
    public Map<String, BigDecimal> getCostByProvider(LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Getting cost by provider from {} to {}", startDate, endDate);

        // In a real implementation, we would filter by date range
        // For now, we'll aggregate all data
        Map<String, BigDecimal> costByProvider = new HashMap<>();

        // Get all executions (with pagination to avoid memory issues)
        Pageable pageable = Pageable.ofSize(1000);
        boolean hasMore = true;
        int page = 0;

        while (hasMore) {
            Page<PromptExecution> executions = executionRepository.findAll(pageable.withPage(page));

            for (PromptExecution execution : executions.getContent()) {
                if (execution.getCost() != null) {
                    String providerId = execution.getProviderId();
                    costByProvider.compute(providerId, (k, v) ->
                            (v == null) ? execution.getCost() : v.add(execution.getCost()));
                }
            }

            hasMore = page < executions.getTotalPages() - 1;
            page++;
        }

        return costByProvider;
    }

    @Override
    public Map<String, Double> getAverageTokensByModel(LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Getting average tokens by model from {} to {}", startDate, endDate);

        try {
            List<Object[]> tokenData = executionRepository.getAverageTokenUsageByProvider();
            Map<String, Double> tokensByModel = new HashMap<>();

            // Process the raw data
            for (Object[] row : tokenData) {
                String modelId = row[0].toString();
                Double avgTokens = (Double) row[1];
                tokensByModel.put(modelId, round(avgTokens));
            }

            return tokensByModel;
        } catch (Exception e) {
            log.error("Error getting average tokens by model", e);
            return Collections.emptyMap();
        }
    }

    @Override
    public Map<String, Double> getSuccessRateByProvider(LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("Getting success rate by provider from {} to {}", startDate, endDate);

        // This would be implemented with a custom query to get execution counts by provider and status
        // For now, we'll use a simplified approach with the data we can access

        Map<String, Long> totalByProvider = new HashMap<>();
        Map<String, Long> successByProvider = new HashMap<>();

        // Get all executions (with pagination to avoid memory issues)
        Pageable pageable = Pageable.ofSize(1000);
        boolean hasMore = true;
        int page = 0;

        while (hasMore) {
            Page<PromptExecution> executions = executionRepository.findAll(pageable.withPage(page));

            for (PromptExecution execution : executions.getContent()) {
                String providerId = execution.getProviderId();
                totalByProvider.compute(providerId, (k, v) -> (v == null) ? 1L : v + 1);

                if (execution.isSuccessful()) {
                    successByProvider.compute(providerId, (k, v) -> (v == null) ? 1L : v + 1);
                }
            }

            hasMore = page < executions.getTotalPages() - 1;
            page++;
        }

        // Calculate success rate for each provider
        Map<String, Double> successRateByProvider = new HashMap<>();
        for (String providerId : totalByProvider.keySet()) {
            long total = totalByProvider.get(providerId);
            long success = successByProvider.getOrDefault(providerId, 0L);
            double rate = total > 0 ? (double) success / total * 100 : 0;
            successRateByProvider.put(providerId, round(rate));
        }

        return successRateByProvider;
    }
}