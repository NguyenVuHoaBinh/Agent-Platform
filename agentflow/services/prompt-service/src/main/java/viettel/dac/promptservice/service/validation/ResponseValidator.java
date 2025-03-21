package viettel.dac.promptservice.service.validation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import viettel.dac.promptservice.dto.validation.ValidationResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validator for LLM responses with multiple validation rules
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ResponseValidator {

    /**
     * Validate an LLM response against specified validation criteria
     *
     * @param response The LLM response text
     * @param validationCriteria Map of validation rules to apply
     * @return Validation result with details
     */
    public ValidationResult validateResponse(String response, Map<String, Object> validationCriteria) {
        if (response == null || response.isEmpty()) {
            ValidationResult result = ValidationResult.builder()
                    .passed(false)
                    .score(0.0)
                    .build();
            result.addIssue("empty_response", "Response is empty", ValidationResult.ValidationSeverity.ERROR);
            return result;
        }

        if (validationCriteria == null || validationCriteria.isEmpty()) {
            // No validation criteria specified, consider it passed
            return ValidationResult.builder()
                    .passed(true)
                    .score(1.0)
                    .build();
        }

        log.debug("Validating response with criteria: {}", validationCriteria);

        ValidationResult result = ValidationResult.builder().build();
        List<Double> scores = new ArrayList<>();
        List<Boolean> ruleResults = new ArrayList<>();

        // Apply each validation rule
        for (Map.Entry<String, Object> entry : validationCriteria.entrySet()) {
            String rule = entry.getKey();
            Object criterion = entry.getValue();

            boolean passed = false;
            String message = "";
            double score = 0.0;

            switch (rule) {
                case "contains":
                    // Check if response contains specified text
                    passed = validateContains(response, criterion, result);
                    message = passed ? "Response contains the required text" : "Response does not contain the required text";
                    score = passed ? 1.0 : 0.0;
                    break;

                case "not_contains":
                    // Check if response does not contain specified text
                    passed = validateNotContains(response, criterion, result);
                    message = passed ? "Response does not contain the forbidden text" : "Response contains forbidden text";
                    score = passed ? 1.0 : 0.0;
                    break;

                case "regex_match":
                    // Check if response matches a regex pattern
                    passed = validateRegexMatch(response, criterion, result);
                    message = passed ? "Response matches the required pattern" : "Response does not match the required pattern";
                    score = passed ? 1.0 : 0.0;
                    break;

                case "min_length":
                    // Check if response meets minimum length
                    passed = validateMinLength(response, criterion, result);
                    message = passed ? "Response meets minimum length requirement" : "Response is too short";
                    score = passed ? 1.0 : 0.0;
                    break;

                case "max_length":
                    // Check if response does not exceed maximum length
                    passed = validateMaxLength(response, criterion, result);
                    message = passed ? "Response is within maximum length" : "Response exceeds maximum length";
                    score = passed ? 1.0 : 0.0;
                    break;

                case "json_format":
                    // Check if response is valid JSON
                    passed = validateJsonFormat(response, result);
                    message = passed ? "Response is valid JSON" : "Response is not valid JSON";
                    score = passed ? 1.0 : 0.0;
                    break;

                case "structure":
                    // Check if response has required structure (sections, etc.)
                    passed = validateStructure(response, criterion, result);
                    message = passed ? "Response has the required structure" : "Response missing required structure";
                    score = passed ? 1.0 : 0.0;
                    break;

                default:
                    // Unknown rule
                    log.warn("Unknown validation rule: {}", rule);
                    result.addIssue(rule, "Unknown validation rule", ValidationResult.ValidationSeverity.WARNING);
                    continue;
            }

            // Record rule result
            result.addRuleResult(rule, passed, message, score);
            scores.add(score);
            ruleResults.add(passed);
        }

        // Calculate overall score and result
        double averageScore = scores.isEmpty() ? 1.0 : scores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        boolean overallPassed = ruleResults.isEmpty() || !ruleResults.contains(false);

        result.setPassed(overallPassed);
        result.setScore(averageScore);

        return result;
    }

    /**
     * Validate that response contains specific text(s)
     */
    private boolean validateContains(String response, Object criterion, ValidationResult result) {
        if (criterion instanceof String) {
            String text = (String) criterion;
            boolean contains = response.contains(text);
            if (!contains) {
                result.addIssue("contains", "Response does not contain: " + text, ValidationResult.ValidationSeverity.ERROR);
            }
            return contains;
        } else if (criterion instanceof List) {
            // Check if response contains all items in the list
            @SuppressWarnings("unchecked")
            List<String> texts = (List<String>) criterion;
            boolean allContained = true;

            for (String text : texts) {
                if (!response.contains(text)) {
                    result.addIssue("contains", "Response does not contain: " + text, ValidationResult.ValidationSeverity.ERROR);
                    allContained = false;
                }
            }

            return allContained;
        } else {
            result.addIssue("contains", "Invalid criterion format for 'contains' rule", ValidationResult.ValidationSeverity.ERROR);
            return false;
        }
    }

    /**
     * Validate that response does not contain specific text(s)
     */
    private boolean validateNotContains(String response, Object criterion, ValidationResult result) {
        if (criterion instanceof String) {
            String text = (String) criterion;
            boolean notContains = !response.contains(text);
            if (!notContains) {
                result.addIssue("not_contains", "Response contains forbidden text: " + text, ValidationResult.ValidationSeverity.ERROR);
            }
            return notContains;
        } else if (criterion instanceof List) {
            // Check if response does not contain any items in the list
            @SuppressWarnings("unchecked")
            List<String> texts = (List<String>) criterion;
            boolean noneContained = true;

            for (String text : texts) {
                if (response.contains(text)) {
                    result.addIssue("not_contains", "Response contains forbidden text: " + text, ValidationResult.ValidationSeverity.ERROR);
                    noneContained = false;
                }
            }

            return noneContained;
        } else {
            result.addIssue("not_contains", "Invalid criterion format for 'not_contains' rule", ValidationResult.ValidationSeverity.ERROR);
            return false;
        }
    }

    /**
     * Validate that response matches a regex pattern
     */
    private boolean validateRegexMatch(String response, Object criterion, ValidationResult result) {
        if (criterion instanceof String) {
            String patternStr = (String) criterion;
            try {
                Pattern pattern = Pattern.compile(patternStr, Pattern.DOTALL);
                Matcher matcher = pattern.matcher(response);
                boolean matches = matcher.find();
                if (!matches) {
                    result.addIssue("regex_match", "Response does not match pattern: " + patternStr, ValidationResult.ValidationSeverity.ERROR);
                }
                return matches;
            } catch (Exception e) {
                result.addIssue("regex_match", "Invalid regex pattern: " + e.getMessage(), ValidationResult.ValidationSeverity.ERROR);
                return false;
            }
        } else {
            result.addIssue("regex_match", "Invalid criterion format for 'regex_match' rule", ValidationResult.ValidationSeverity.ERROR);
            return false;
        }
    }

    /**
     * Validate that response meets minimum length
     */
    private boolean validateMinLength(String response, Object criterion, ValidationResult result) {
        try {
            int minLength = Integer.parseInt(criterion.toString());
            boolean valid = response.length() >= minLength;
            if (!valid) {
                result.addIssue("min_length", "Response length (" + response.length() + ") is less than minimum required (" + minLength + ")", ValidationResult.ValidationSeverity.ERROR);
            }
            return valid;
        } catch (NumberFormatException e) {
            result.addIssue("min_length", "Invalid criterion format for 'min_length' rule", ValidationResult.ValidationSeverity.ERROR);
            return false;
        }
    }

    /**
     * Validate that response does not exceed maximum length
     */
    private boolean validateMaxLength(String response, Object criterion, ValidationResult result) {
        try {
            int maxLength = Integer.parseInt(criterion.toString());
            boolean valid = response.length() <= maxLength;
            if (!valid) {
                result.addIssue("max_length", "Response length (" + response.length() + ") exceeds maximum allowed (" + maxLength + ")", ValidationResult.ValidationSeverity.ERROR);
            }
            return valid;
        } catch (NumberFormatException e) {
            result.addIssue("max_length", "Invalid criterion format for 'max_length' rule", ValidationResult.ValidationSeverity.ERROR);
            return false;
        }
    }

    /**
     * Validate that response is valid JSON
     */
    private boolean validateJsonFormat(String response, ValidationResult result) {
        try {
            // Try to parse as JSON
            new com.fasterxml.jackson.databind.ObjectMapper().readTree(response);
            return true;
        } catch (Exception e) {
            result.addIssue("json_format", "Response is not valid JSON: " + e.getMessage(), ValidationResult.ValidationSeverity.ERROR);
            return false;
        }
    }

    /**
     * Validate that response has required structure (sections, headers, etc.)
     */
    private boolean validateStructure(String response, Object criterion, ValidationResult result) {
        if (criterion instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> structure = (Map<String, Object>) criterion;
            boolean valid = true;

            // Check if required sections are present
            if (structure.containsKey("sections")) {
                @SuppressWarnings("unchecked")
                List<String> sections = (List<String>) structure.get("sections");
                for (String section : sections) {
                    // Simple heuristic: check if section header exists
                    if (!response.contains(section)) {
                        result.addIssue("structure", "Response missing required section: " + section, ValidationResult.ValidationSeverity.ERROR);
                        valid = false;
                    }
                }
            }

            // Check if required headers are present
            if (structure.containsKey("headers")) {
                @SuppressWarnings("unchecked")
                List<String> headers = (List<String>) structure.get("headers");
                for (String header : headers) {
                    // Look for markdown headers (# Header) or uppercase headers
                    Pattern pattern = Pattern.compile("(?m)(^#+ " + Pattern.quote(header) + "\\s*$)|(^" + Pattern.quote(header.toUpperCase()) + "\\s*$)");
                    Matcher matcher = pattern.matcher(response);
                    if (!matcher.find()) {
                        result.addIssue("structure", "Response missing required header: " + header, ValidationResult.ValidationSeverity.ERROR);
                        valid = false;
                    }
                }
            }

            return valid;
        } else {
            result.addIssue("structure", "Invalid criterion format for 'structure' rule", ValidationResult.ValidationSeverity.ERROR);
            return false;
        }
    }
}