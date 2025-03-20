package viettel.dac.promptservice.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.cache.annotation.Cacheable;
import viettel.dac.promptservice.model.enums.ParameterType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

/**
 * Prompt parameter entity with validation capabilities
 */
@Entity
@Table(name = "prompt_parameters", indexes = {
        @Index(name = "idx_parameter_version", columnList = "version_id"),
        @Index(name = "idx_parameter_type", columnList = "parameter_type"),
        @Index(name = "idx_parameter_required", columnList = "required")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Cacheable("promptParameters")
public class PromptParameter extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "version_id", nullable = false)
    private PromptVersion version;

    @NotBlank(message = "Parameter name is required")
    @Size(min = 1, max = 100, message = "Parameter name must be between 1 and 100 characters")
    @Column(nullable = false)
    private String name;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    @Column(length = 500)
    private String description;

    @NotNull(message = "Parameter type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "parameter_type", nullable = false)
    private ParameterType parameterType;

    @Column(name = "default_value", columnDefinition = "TEXT")
    private String defaultValue;

    @Column(nullable = false)
    private boolean required;

    @Column(name = "validation_pattern")
    private String validationPattern;

    /**
     * Validate a parameter value against this parameter's constraints
     *
     * @param value The value to validate
     * @return true if the value is valid
     */
    public boolean validateValue(Object value) {
        // Check for required parameters
        if (required && (value == null || String.valueOf(value).isEmpty())) {
            return false;
        }

        // Null is valid for non-required parameters
        if (value == null) {
            return !required;
        }

        String strValue = String.valueOf(value);

        // Type-specific validation
        switch (parameterType) {
            case STRING:
                if (validationPattern != null && !strValue.matches(validationPattern)) {
                    return false;
                }
                return true;

            case NUMBER:
                try {
                    Double.parseDouble(strValue);
                    return true;
                } catch (NumberFormatException e) {
                    return false;
                }

            case BOOLEAN:
                String lowerValue = strValue.toLowerCase();
                return lowerValue.equals("true") || lowerValue.equals("false") ||
                        lowerValue.equals("1") || lowerValue.equals("0") ||
                        lowerValue.equals("yes") || lowerValue.equals("no");

            case DATE:
                try {
                    LocalDate.parse(strValue);
                    return true;
                } catch (DateTimeParseException e) {
                    return false;
                }

            case DATETIME:
                try {
                    LocalDateTime.parse(strValue);
                    return true;
                } catch (DateTimeParseException e) {
                    return false;
                }

            case ARRAY:
                // Basic validation: must start with [ and end with ]
                return strValue.startsWith("[") && strValue.endsWith("]");

            case OBJECT:
                // Basic validation: must start with { and end with }
                return strValue.startsWith("{") && strValue.endsWith("}");

            default:
                return false;
        }
    }

    /**
     * Convert parameter value to appropriate type based on parameter type
     *
     * @param value The value to convert
     * @return The converted value
     */
    public Object convertValue(String value) {
        if (value == null) {
            return null;
        }

        switch (parameterType) {
            case NUMBER:
                try {
                    if (value.contains(".")) {
                        return Double.parseDouble(value);
                    } else {
                        return Integer.parseInt(value);
                    }
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid number format for parameter: " + name);
                }

            case BOOLEAN:
                String lowerValue = value.toLowerCase();
                if (lowerValue.equals("true") || lowerValue.equals("1") || lowerValue.equals("yes")) {
                    return Boolean.TRUE;
                } else if (lowerValue.equals("false") || lowerValue.equals("0") || lowerValue.equals("no")) {
                    return Boolean.FALSE;
                } else {
                    throw new IllegalArgumentException("Invalid boolean value for parameter: " + name);
                }

            case DATE:
                try {
                    return LocalDate.parse(value);
                } catch (DateTimeParseException e) {
                    throw new IllegalArgumentException("Invalid date format for parameter: " + name);
                }

            case DATETIME:
                try {
                    return LocalDateTime.parse(value);
                } catch (DateTimeParseException e) {
                    throw new IllegalArgumentException("Invalid datetime format for parameter: " + name);
                }

            default:
                return value;
        }
    }

    /**
     * Get default value converted to appropriate type
     *
     * @return Converted default value or null if none exists
     */
    public Object getTypedDefaultValue() {
        if (defaultValue == null || defaultValue.isEmpty()) {
            return null;
        }

        return convertValue(defaultValue);
    }
}