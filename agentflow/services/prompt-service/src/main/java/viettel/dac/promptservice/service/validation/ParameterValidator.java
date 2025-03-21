package viettel.dac.promptservice.service.validation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import viettel.dac.promptservice.dto.validation.ParameterValidationResult;
import viettel.dac.promptservice.model.entity.PromptParameter;
import viettel.dac.promptservice.model.entity.PromptVersion;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Validator for prompt parameters with comprehensive validation rules
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ParameterValidator {

    /**
     * Validate parameter values against a prompt version's parameter definitions
     *
     * @param version The prompt version containing parameter definitions
     * @param parameterValues The parameter values to validate
     * @return Validation result with details
     */
    public ParameterValidationResult validateParameters(PromptVersion version, Map<String, Object> parameterValues) {
        if (version == null) {
            throw new IllegalArgumentException("Version cannot be null");
        }

        log.debug("Validating parameters for version {}: {}", version.getId(), parameterValues);

        // Convert version parameters to a map for easy lookup
        Map<String, PromptParameter> parameterMap = version.getParameters().stream()
                .collect(Collectors.toMap(PromptParameter::getName, p -> p));

        // Track issues found during validation
        List<ParameterValidationResult.ValidationIssue> issues = new ArrayList<>();

        // Track unknown parameters (provided but not defined)
        List<String> unknownParameters = new ArrayList<>();

        // Track missing required parameters
        List<String> missingRequired = new ArrayList<>();

        // Result map with validated and processed values
        Map<String, Object> validatedValues = new HashMap<>();

        // Check for unknown parameters (provided but not defined)
        if (parameterValues != null) {
            for (String paramName : parameterValues.keySet()) {
                if (!parameterMap.containsKey(paramName)) {
                    unknownParameters.add(paramName);
                }
            }
        }

        // Validate each defined parameter
        for (PromptParameter param : version.getParameters()) {
            String paramName = param.getName();
            Object paramValue = parameterValues != null ? parameterValues.get(paramName) : null;

            // Handle required parameters
            if (param.isRequired() && (paramValue == null || paramValue.toString().trim().isEmpty())) {
                missingRequired.add(paramName);
                issues.add(ParameterValidationResult.ValidationIssue.builder()
                        .parameter(paramName)
                        .message("Required parameter is missing")
                        .severity(ParameterValidationResult.ValidationSeverity.ERROR)
                        .build());
                continue;
            }

            // Skip validation for null values if parameter is optional
            if (paramValue == null && !param.isRequired()) {
                // Add default value if specified
                if (param.getDefaultValue() != null && !param.getDefaultValue().isEmpty()) {
                    Object defaultValue = param.getTypedDefaultValue();
                    validatedValues.put(paramName, defaultValue);
                }
                continue;
            }

            // Process and validate value if present
            if (paramValue != null) {
                try {
                    // Validate by type
                    if (!validateByType(param, paramValue, issues)) {
                        // Type validation failed, add default if available
                        if (param.getDefaultValue() != null && !param.getDefaultValue().isEmpty()) {
                            validatedValues.put(paramName, param.getTypedDefaultValue());
                        }
                        continue;
                    }

                    // Validate by pattern if specified
                    if (param.getValidationPattern() != null && !param.getValidationPattern().isEmpty()) {
                        String strValue = paramValue.toString();
                        if (!strValue.matches(param.getValidationPattern())) {
                            issues.add(ParameterValidationResult.ValidationIssue.builder()
                                    .parameter(paramName)
                                    .message("Value does not match required pattern: " + param.getValidationPattern())
                                    .severity(ParameterValidationResult.ValidationSeverity.ERROR)
                                    .build());

                            // Add default if available
                            if (param.getDefaultValue() != null && !param.getDefaultValue().isEmpty()) {
                                validatedValues.put(paramName, param.getTypedDefaultValue());
                            }
                            continue;
                        }
                    }

                    // Convert value to appropriate type if needed
                    Object convertedValue = convertToAppropriateType(param, paramValue);
                    validatedValues.put(paramName, convertedValue);

                } catch (Exception e) {
                    log.debug("Validation exception for parameter {}: {}", paramName, e.getMessage());
                    issues.add(ParameterValidationResult.ValidationIssue.builder()
                            .parameter(paramName)
                            .message("Validation error: " + e.getMessage())
                            .severity(ParameterValidationResult.ValidationSeverity.ERROR)
                            .build());

                    // Add default if available
                    if (param.getDefaultValue() != null && !param.getDefaultValue().isEmpty()) {
                        validatedValues.put(paramName, param.getTypedDefaultValue());
                    }
                }
            }
        }

        // Add unknown parameters as warnings
        for (String unknown : unknownParameters) {
            issues.add(ParameterValidationResult.ValidationIssue.builder()
                    .parameter(unknown)
                    .message("Parameter is not defined in the prompt version")
                    .severity(ParameterValidationResult.ValidationSeverity.WARNING)
                    .build());
        }

        boolean passed = missingRequired.isEmpty() &&
                issues.stream().noneMatch(i -> i.getSeverity() == ParameterValidationResult.ValidationSeverity.ERROR);

        return ParameterValidationResult.builder()
                .valid(passed)
                .issues(issues)
                .unknownParameters(unknownParameters)
                .missingRequired(missingRequired)
                .validatedValues(validatedValues)
                .build();
    }

    /**
     * Validate parameter value by its type
     */
    private boolean validateByType(PromptParameter param, Object value, List<ParameterValidationResult.ValidationIssue> issues) {
        String paramName = param.getName();
        String strValue = value.toString();

        try {
            switch (param.getParameterType()) {
                case STRING:
                    // String values are always valid
                    return true;

                case NUMBER:
                    // Try parsing as number
                    try {
                        if (strValue.contains(".")) {
                            Double.parseDouble(strValue);
                        } else {
                            Long.parseLong(strValue);
                        }
                        return true;
                    } catch (NumberFormatException e) {
                        issues.add(ParameterValidationResult.ValidationIssue.builder()
                                .parameter(paramName)
                                .message("Value is not a valid number")
                                .severity(ParameterValidationResult.ValidationSeverity.ERROR)
                                .build());
                        return false;
                    }

                case BOOLEAN:
                    // Check if value is a valid boolean
                    String lowerValue = strValue.toLowerCase();
                    if (lowerValue.equals("true") || lowerValue.equals("false") ||
                            lowerValue.equals("1") || lowerValue.equals("0") ||
                            lowerValue.equals("yes") || lowerValue.equals("no")) {
                        return true;
                    } else {
                        issues.add(ParameterValidationResult.ValidationIssue.builder()
                                .parameter(paramName)
                                .message("Value is not a valid boolean")
                                .severity(ParameterValidationResult.ValidationSeverity.ERROR)
                                .build());
                        return false;
                    }

                case DATE:
                    // Check if value is a valid date
                    try {
                        LocalDate.parse(strValue);
                        return true;
                    } catch (DateTimeParseException e) {
                        issues.add(ParameterValidationResult.ValidationIssue.builder()
                                .parameter(paramName)
                                .message("Value is not a valid date (use ISO format YYYY-MM-DD)")
                                .severity(ParameterValidationResult.ValidationSeverity.ERROR)
                                .build());
                        return false;
                    }

                case DATETIME:
                    // Check if value is a valid datetime
                    try {
                        LocalDateTime.parse(strValue);
                        return true;
                    } catch (DateTimeParseException e) {
                        issues.add(ParameterValidationResult.ValidationIssue.builder()
                                .parameter(paramName)
                                .message("Value is not a valid datetime (use ISO format YYYY-MM-DDTHH:MM:SS)")
                                .severity(ParameterValidationResult.ValidationSeverity.ERROR)
                                .build());
                        return false;
                    }

                case ARRAY:
                    // Check if value looks like a JSON array
                    if (strValue.startsWith("[") && strValue.endsWith("]")) {
                        return true;
                    } else {
                        issues.add(ParameterValidationResult.ValidationIssue.builder()
                                .parameter(paramName)
                                .message("Value is not a valid array (must be JSON format)")
                                .severity(ParameterValidationResult.ValidationSeverity.ERROR)
                                .build());
                        return false;
                    }

                case OBJECT:
                    // Check if value looks like a JSON object
                    if (strValue.startsWith("{") && strValue.endsWith("}")) {
                        return true;
                    } else {
                        issues.add(ParameterValidationResult.ValidationIssue.builder()
                                .parameter(paramName)
                                .message("Value is not a valid object (must be JSON format)")
                                .severity(ParameterValidationResult.ValidationSeverity.ERROR)
                                .build());
                        return false;
                    }

                default:
                    issues.add(ParameterValidationResult.ValidationIssue.builder()
                            .parameter(paramName)
                            .message("Unknown parameter type: " + param.getParameterType())
                            .severity(ParameterValidationResult.ValidationSeverity.ERROR)
                            .build());
                    return false;
            }
        } catch (Exception e) {
            issues.add(ParameterValidationResult.ValidationIssue.builder()
                    .parameter(paramName)
                    .message("Validation error: " + e.getMessage())
                    .severity(ParameterValidationResult.ValidationSeverity.ERROR)
                    .build());
            return false;
        }
    }

    /**
     * Convert a parameter value to its appropriate type
     */
    private Object convertToAppropriateType(PromptParameter param, Object value) {
        if (value == null) return null;

        String strValue = value.toString();

        switch (param.getParameterType()) {
            case NUMBER:
                if (strValue.contains(".")) {
                    return Double.parseDouble(strValue);
                } else {
                    try {
                        return Integer.parseInt(strValue);
                    } catch (NumberFormatException e) {
                        return Long.parseLong(strValue);
                    }
                }

            case BOOLEAN:
                String lowerValue = strValue.toLowerCase();
                return lowerValue.equals("true") || lowerValue.equals("1") || lowerValue.equals("yes");

            case DATE:
                return LocalDate.parse(strValue);

            case DATETIME:
                return LocalDateTime.parse(strValue);

            case ARRAY:
            case OBJECT:
            case STRING:
            default:
                return strValue;
        }
    }
}