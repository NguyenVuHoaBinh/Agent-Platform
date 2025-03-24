package viettel.dac.promptservice.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Generic wrapper for various types of metrics data
 *
 * @param <T> The type of metrics data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricsResponse<T> {

    /**
     * Type of metrics (e.g., template_usage, version_performance)
     */
    private String metricType;

    /**
     * Period for which metrics are calculated (e.g., daily, weekly, all_time)
     */
    private String period;

    /**
     * Start time of the data range
     */
    private LocalDateTime startTime;

    /**
     * End time of the data range
     */
    private LocalDateTime endTime;

    /**
     * The metrics data
     */
    private T data;

    /**
     * Additional metadata about the metrics
     */
    private Map<String, Object> metadata;
}