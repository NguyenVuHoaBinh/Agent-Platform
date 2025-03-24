package viettel.dac.promptservice.dto.optimization;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * DTO for optimization results
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptimizationResult {

    /**
     * Original prompt text
     */
    private String originalText;

    /**
     * Optimized prompt text
     */
    private String optimizedText;

    /**
     * List of suggestions for improvement
     */
    @Builder.Default
    private List<Suggestion> suggestions = new ArrayList<>();

    /**
     * Overall optimization score (0-100)
     */
    private Integer score;

    /**
     * Potential impact metrics
     */
    private Map<String, Object> potentialImpact;

    /**
     * Optimization recommendation
     */
    private String recommendation;

    /**
     * Performance metrics before optimization
     */
    private Map<String, Object> currentMetrics;

    /**
     * Projected performance metrics after optimization
     */
    private Map<String, Object> projectedMetrics;

    /**
     * Whether automatic optimization was applied
     */
    private boolean automaticallyOptimized;

    /**
     * Add a suggestion to the result
     */
    public void addSuggestion(Suggestion suggestion) {
        if (this.suggestions == null) {
            this.suggestions = new ArrayList<>();
        }
        this.suggestions.add(suggestion);
    }

    /**
     * Calculate overall score based on suggestions
     */
    public void calculateScore() {
        if (suggestions == null || suggestions.isEmpty()) {
            this.score = 100; // Perfect if no suggestions
            return;
        }

        // Base score is 100, subtract points for each issue
        int baseScore = 100;
        int totalIssues = suggestions.size();
        int criticalIssues = (int) suggestions.stream()
                .filter(s -> s.getSeverity() == Severity.CRITICAL)
                .count();
        int majorIssues = (int) suggestions.stream()
                .filter(s -> s.getSeverity() == Severity.MAJOR)
                .count();
        int minorIssues = (int) suggestions.stream()
                .filter(s -> s.getSeverity() == Severity.MINOR)
                .count();

        // Deduct points based on severity
        baseScore -= (criticalIssues * 15); // 15 points per critical issue
        baseScore -= (majorIssues * 7);     // 7 points per major issue
        baseScore -= (minorIssues * 3);     // 3 points per minor issue

        // Ensure score is between 0 and 100
        this.score = Math.max(0, Math.min(100, baseScore));
    }

    /**
     * Suggestion for prompt improvement
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Suggestion {
        /**
         * Suggestion type
         */
        private SuggestionType type;

        /**
         * Suggestion description
         */
        private String description;

        /**
         * Location in the text (if applicable)
         */
        private TextLocation location;

        /**
         * Suggestion severity
         */
        private Severity severity;

        /**
         * Original text fragment
         */
        private String originalText;

        /**
         * Suggested replacement text
         */
        private String suggestedText;

        /**
         * Explanation for the suggestion
         */
        private String explanation;

        /**
         * Expected improvement metrics
         */
        private Map<String, Object> expectedImprovement;
    }

    /**
     * Location in text
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TextLocation {
        private int startIndex;
        private int endIndex;
        private int startLine;
        private int endLine;
    }

    /**
     * Suggestion severity
     */
    public enum Severity {
        CRITICAL,
        MAJOR,
        MINOR,
        INFO
    }
}