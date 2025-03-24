package viettel.dac.promptservice.dto.optimization;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DTO for prompt optimization request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptOptimizationRequest {

    /**
     * Types of suggestions to apply
     */
    @Builder.Default
    private List<SuggestionType> suggestionTypes = new ArrayList<>();

    /**
     * Whether to apply automatic optimization
     */
    @Builder.Default
    private boolean applyAutomatically = false;

    /**
     * Whether to create a new version with optimized prompt
     */
    @Builder.Default
    private boolean createNewVersion = false;

    /**
     * Model ID to use for optimization
     */
    private String modelId;

    /**
     * Provider ID to use for optimization
     */
    private String providerId;

    /**
     * Optimization strategies to apply
     */
    @Builder.Default
    private Map<String, Boolean> strategies = new HashMap<>();

    /**
     * Custom optimization parameters
     */
    @Builder.Default
    private Map<String, Object> parameters = new HashMap<>();

    /**
     * Maximum token budget
     */
    private Integer maxTokens;

    /**
     * Target optimization goals
     */
    @Builder.Default
    private List<OptimizationGoal> goals = new ArrayList<>();

    /**
     * Whether to test optimization impact
     */
    @Builder.Default
    private boolean measureImpact = false;

    /**
     * Number of test samples for impact measurement
     */
    @Builder.Default
    private int testSampleCount = 10;

    /**
     * Whether to run optimizations asynchronously
     */
    @Builder.Default
    private boolean async = false;

    /**
     * Optimization target (what aspects to prioritize)
     */
    public enum OptimizationGoal {
        TOKEN_REDUCTION,
        RESPONSE_QUALITY,
        RESPONSE_TIME,
        COST_REDUCTION,
        ERROR_REDUCTION
    }
}