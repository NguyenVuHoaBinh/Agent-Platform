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
 * DTO representing the result of parameter validation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParameterValidationResult {

    /**
     * Overall validation result (true if all required parameters are valid)
     */
    private boolean valid;

    /**
     * List of validation issues found
     */
    @Builder.Default
    private List<ValidationIssue> issues = new ArrayList<>();

    /**
     * List of parameter names that were provided but not defined
     */
    @Builder.Default
    private List<String> unknownParameters = new ArrayList<>();

    /**
     * List of required parameter names that were missing
     */
    @Builder.Default
    private List<String> missingRequired = new ArrayList<>();

    /**
     * Map of parameter names to their validated and converted values
     */
    @Builder.Default
    private Map<String, Object> validatedValues = new HashMap<>();

    /**
     * Add a validation issue
     */
    public void addIssue(String parameter, String message, ValidationSeverity severity) {
        issues.add(ValidationIssue.builder()
                .parameter(parameter)
                .message(message)
                .severity(severity)
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
        private String parameter;
        private String message;
        private ValidationSeverity severity;
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