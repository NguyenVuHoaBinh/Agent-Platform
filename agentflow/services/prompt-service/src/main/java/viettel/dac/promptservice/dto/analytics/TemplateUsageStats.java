package viettel.dac.promptservice.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Statistics about prompt template usage and performance
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateUsageStats {

    /**
     * Template ID
     */
    private String templateId;

    /**
     * Template name
     */
    private String templateName;

    /**
     * Total number of executions
     */
    private Long totalExecutions;

    /**
     * Total number of successful executions
     */
    private Long successfulExecutions;

    /**
     * Total number of failed executions
     */
    private Long failedExecutions;

    /**
     * Success rate (percentage)
     */
    private Double successRate;

    /**
     * Total tokens consumed
     */
    private Long totalTokens;

    /**
     * Total cost incurred
     */
    private BigDecimal totalCost;

    /**
     * Average response time in milliseconds
     */
    private Double averageResponseTime;

    /**
     * Executions by model
     */
    private Map<String, Long> executionsByModel;

    /**
     * Executions by provider
     */
    private Map<String, Long> executionsByProvider;

    /**
     * Time series data for executions over time
     */
    private List<TimeSeriesDataPoint> executionsOverTime;

    /**
     * First execution timestamp
     */
    private LocalDateTime firstExecution;

    /**
     * Last execution timestamp
     */
    private LocalDateTime lastExecution;

    /**
     * Time series data point
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeSeriesDataPoint {
        private LocalDateTime timestamp;
        private Long value;
    }
}