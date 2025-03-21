package viettel.dac.promptservice.dto.validation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DTO representing the result of validating a prompt response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResult {

    /**
     * Overall validation passed
     */
    private boolean passed;

    /**
     * Score between 0.0 and 1.0 representing validation quality
     */
    private Double score;

    /**
     * List of validation issues found
     */
    @Builder.Default
    private List<ValidationIssue> issues = new ArrayList<>();

    /**
     * Detailed results for each validation rule
     */
    @Builder.Default
    private Map<String, RuleResult> ruleResults = new HashMap<>();

    /**
     * Add a validation issue
     */
    public void addIssue(String rule, String message, ValidationSeverity severity) {
        issues.add(ValidationIssue.builder()
                .rule(rule)
                .message(message)
                .severity(severity)
                .build());
    }

    /**
     * Add a rule result
     */
    public void addRuleResult(String rule, boolean passed, String message, Double score) {
        ruleResults.put(rule, RuleResult.builder()
                .passed(passed)
                .message(message)
                .score(score)
                .build());
    }

    /**
     * Inner class for individual validation issues
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationIssue {
        private String rule;
        private String message;
        private ValidationSeverity severity;
    }

    /**
     * Inner class for detailed rule results
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RuleResult {
        private boolean passed;
        private String message;
        private Double score;
    }

    /**
     * Enum for validation issue severity
     */
    public enum ValidationSeverity {
        INFO,
        WARNING,
        ERROR
    }
}