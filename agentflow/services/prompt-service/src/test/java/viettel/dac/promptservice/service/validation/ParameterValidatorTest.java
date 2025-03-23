package viettel.dac.promptservice.service.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import viettel.dac.promptservice.dto.validation.ParameterValidationResult;
import viettel.dac.promptservice.model.entity.PromptParameter;
import viettel.dac.promptservice.model.entity.PromptVersion;
import viettel.dac.promptservice.model.enums.ParameterType;
import viettel.dac.promptservice.service.validation.ParameterValidator;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ParameterValidatorTest {

    @Mock
    private PromptVersion mockVersion;

    private ParameterValidator validator;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        validator = new ParameterValidator();
    }

    @Test
    public void testValidateParameters_NullVersion() {
        // Should throw exception when version is null
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> validator.validateParameters(null, Collections.emptyMap())
        );

        assertEquals("Version cannot be null", exception.getMessage());
    }

    @Test
    public void testValidateParameters_NullParameterValues() {
        // Setup a version with required parameters
        Set<PromptParameter> parameters = new HashSet<>();
        parameters.add(createParameter("param1", "Parameter 1", ParameterType.STRING, true, null, null));

        when(mockVersion.getParameters()).thenReturn(parameters);

        // Test with null parameter values
        ParameterValidationResult result = validator.validateParameters(mockVersion, null);

        // Should not be valid due to missing required parameter
        assertFalse(result.isValid());
        assertEquals(1, result.getMissingRequired().size());
        assertEquals("param1", result.getMissingRequired().get(0));
        assertEquals(0, result.getUnknownParameters().size());
    }

    @Test
    public void testValidateParameters_EmptyParameterValues() {
        // Setup a version with required parameters
        Set<PromptParameter> parameters = new HashSet<>();
        parameters.add(createParameter("param1", "Parameter 1", ParameterType.STRING, true, null, null));

        when(mockVersion.getParameters()).thenReturn(parameters);

        // Test with empty parameter values
        ParameterValidationResult result = validator.validateParameters(mockVersion, Collections.emptyMap());

        // Should not be valid due to missing required parameter
        assertFalse(result.isValid());
        assertEquals(1, result.getMissingRequired().size());
        assertEquals("param1", result.getMissingRequired().get(0));
        assertEquals(0, result.getUnknownParameters().size());
    }

    @Test
    public void testValidateParameters_AllRequiredParametersPresent() {
        // Setup a version with required and optional parameters
        Set<PromptParameter> parameters = new HashSet<>();
        parameters.add(createParameter("param1", "Parameter 1", ParameterType.STRING, true, null, null));
        parameters.add(createParameter("param2", "Parameter 2", ParameterType.NUMBER, false, null, null));

        when(mockVersion.getParameters()).thenReturn(parameters);

        // Test with all required parameters
        Map<String, Object> parameterValues = new HashMap<>();
        parameterValues.put("param1", "value1");

        ParameterValidationResult result = validator.validateParameters(mockVersion, parameterValues);

        // Should be valid since all required parameters are provided
        assertTrue(result.isValid());
        assertEquals(0, result.getMissingRequired().size());
        assertEquals(0, result.getUnknownParameters().size());
        assertEquals(1, result.getValidatedValues().size());
        assertEquals("value1", result.getValidatedValues().get("param1"));
    }

    @Test
    public void testValidateParameters_MissingRequiredParameter() {
        // Setup a version with multiple required parameters
        Set<PromptParameter> parameters = new HashSet<>();
        parameters.add(createParameter("param1", "Parameter 1", ParameterType.STRING, true, null, null));
        parameters.add(createParameter("param2", "Parameter 2", ParameterType.STRING, true, null, null));

        when(mockVersion.getParameters()).thenReturn(parameters);

        // Test with missing required parameter
        Map<String, Object> parameterValues = new HashMap<>();
        parameterValues.put("param1", "value1");

        ParameterValidationResult result = validator.validateParameters(mockVersion, parameterValues);

        // Should not be valid due to missing param2
        assertFalse(result.isValid());
        assertEquals(1, result.getMissingRequired().size());
        assertEquals("param2", result.getMissingRequired().get(0));
        assertEquals(0, result.getUnknownParameters().size());
        assertEquals(1, result.getIssues().size());
        assertEquals(ParameterValidationResult.ValidationSeverity.ERROR, result.getIssues().get(0).getSeverity());
    }

    @Test
    public void testValidateParameters_UnknownParameter() {
        // Setup a version with parameters
        Set<PromptParameter> parameters = new HashSet<>();
        parameters.add(createParameter("param1", "Parameter 1", ParameterType.STRING, false, null, null));

        when(mockVersion.getParameters()).thenReturn(parameters);

        // Test with unknown parameter
        Map<String, Object> parameterValues = new HashMap<>();
        parameterValues.put("param1", "value1");
        parameterValues.put("unknown", "value2");

        ParameterValidationResult result = validator.validateParameters(mockVersion, parameterValues);

        // Should be valid but with unknown parameter warning
        assertTrue(result.isValid());
        assertEquals(0, result.getMissingRequired().size());
        assertEquals(1, result.getUnknownParameters().size());
        assertEquals("unknown", result.getUnknownParameters().get(0));
        assertEquals(1, result.getIssues().size());
        assertEquals(ParameterValidationResult.ValidationSeverity.WARNING, result.getIssues().get(0).getSeverity());
    }

    @Test
    public void testValidateParameters_StringType() {
        // Setup a version with string parameter
        Set<PromptParameter> parameters = new HashSet<>();
        parameters.add(createParameter("param1", "Parameter 1", ParameterType.STRING, false, null, null));

        when(mockVersion.getParameters()).thenReturn(parameters);

        // Test with string value
        Map<String, Object> parameterValues = new HashMap<>();
        parameterValues.put("param1", "value1");

        ParameterValidationResult result = validator.validateParameters(mockVersion, parameterValues);

        // Should be valid
        assertTrue(result.isValid());
        assertEquals(1, result.getValidatedValues().size());
        assertEquals("value1", result.getValidatedValues().get("param1"));
    }

    @Test
    public void testValidateParameters_NumberType_Valid() {
        // Setup a version with number parameter
        Set<PromptParameter> parameters = new HashSet<>();
        parameters.add(createParameter("param1", "Parameter 1", ParameterType.NUMBER, false, null, null));

        when(mockVersion.getParameters()).thenReturn(parameters);

        // Test with valid number values
        Map<String, Object> parameterValues = new HashMap<>();
        parameterValues.put("param1", "123");

        ParameterValidationResult result = validator.validateParameters(mockVersion, parameterValues);

        // Should be valid with integer value
        assertTrue(result.isValid());
        assertEquals(1, result.getValidatedValues().size());
        assertEquals(123, result.getValidatedValues().get("param1"));

        // Test with decimal number
        parameterValues.put("param1", "123.45");

        result = validator.validateParameters(mockVersion, parameterValues);

        // Should be valid with double value
        assertTrue(result.isValid());
        assertEquals(1, result.getValidatedValues().size());
        assertEquals(123.45, result.getValidatedValues().get("param1"));
    }

    @Test
    public void testValidateParameters_NumberType_Invalid() {
        // Setup a version with number parameter
        Set<PromptParameter> parameters = new HashSet<>();
        parameters.add(createParameter("param1", "Parameter 1", ParameterType.NUMBER, false, null, null));

        when(mockVersion.getParameters()).thenReturn(parameters);

        // Test with invalid number value
        Map<String, Object> parameterValues = new HashMap<>();
        parameterValues.put("param1", "not-a-number");

        ParameterValidationResult result = validator.validateParameters(mockVersion, parameterValues);

        // Should not be valid due to invalid number format
        assertTrue(result.isValid()); // It should be valid overall since the parameter is optional
        assertEquals(1, result.getIssues().size());
        assertEquals(ParameterValidationResult.ValidationSeverity.ERROR, result.getIssues().get(0).getSeverity());
        assertEquals(0, result.getValidatedValues().size());
    }

    @Test
    public void testValidateParameters_BooleanType_Valid() {
        // Setup a version with boolean parameter
        Set<PromptParameter> parameters = new HashSet<>();
        parameters.add(createParameter("param1", "Parameter 1", ParameterType.BOOLEAN, false, null, null));

        when(mockVersion.getParameters()).thenReturn(parameters);

        // Test with valid boolean values in different formats
        Map<String, Object> parameterValues = new HashMap<>();

        // Test "true"
        parameterValues.put("param1", "true");
        ParameterValidationResult result = validator.validateParameters(mockVersion, parameterValues);
        assertTrue(result.isValid());
        assertEquals(1, result.getValidatedValues().size());
        assertEquals(true, result.getValidatedValues().get("param1"));

        // Test "false"
        parameterValues.put("param1", "false");
        result = validator.validateParameters(mockVersion, parameterValues);
        assertTrue(result.isValid());
        assertEquals(1, result.getValidatedValues().size());
        assertEquals(false, result.getValidatedValues().get("param1"));

        // Test "1" (should be treated as true)
        parameterValues.put("param1", "1");
        result = validator.validateParameters(mockVersion, parameterValues);
        assertTrue(result.isValid());
        assertEquals(1, result.getValidatedValues().size());
        assertEquals(true, result.getValidatedValues().get("param1"));

        // Test "0" (should be treated as false)
        parameterValues.put("param1", "0");
        result = validator.validateParameters(mockVersion, parameterValues);
        assertTrue(result.isValid());
        assertEquals(1, result.getValidatedValues().size());
        assertEquals(false, result.getValidatedValues().get("param1"));

        // Test "yes" (should be treated as true)
        parameterValues.put("param1", "yes");
        result = validator.validateParameters(mockVersion, parameterValues);
        assertTrue(result.isValid());
        assertEquals(1, result.getValidatedValues().size());
        assertEquals(true, result.getValidatedValues().get("param1"));

        // Test "no" (should be treated as false)
        parameterValues.put("param1", "no");
        result = validator.validateParameters(mockVersion, parameterValues);
        assertTrue(result.isValid());
        assertEquals(1, result.getValidatedValues().size());
        assertEquals(false, result.getValidatedValues().get("param1"));
    }

    @Test
    public void testValidateParameters_BooleanType_Invalid() {
        // Setup a version with boolean parameter
        Set<PromptParameter> parameters = new HashSet<>();
        parameters.add(createParameter("param1", "Parameter 1", ParameterType.BOOLEAN, false, null, null));

        when(mockVersion.getParameters()).thenReturn(parameters);

        // Test with invalid boolean value
        Map<String, Object> parameterValues = new HashMap<>();
        parameterValues.put("param1", "not-a-boolean");

        ParameterValidationResult result = validator.validateParameters(mockVersion, parameterValues);

        // Should not validate successfully
        assertTrue(result.isValid()); // It should be valid overall since the parameter is optional
        assertEquals(1, result.getIssues().size());
        assertEquals(ParameterValidationResult.ValidationSeverity.ERROR, result.getIssues().get(0).getSeverity());
        assertEquals(0, result.getValidatedValues().size());
    }

    @Test
    public void testValidateParameters_DateType_Valid() {
        // Setup a version with date parameter
        Set<PromptParameter> parameters = new HashSet<>();
        parameters.add(createParameter("param1", "Parameter 1", ParameterType.DATE, false, null, null));

        when(mockVersion.getParameters()).thenReturn(parameters);

        // Test with valid date
        Map<String, Object> parameterValues = new HashMap<>();
        parameterValues.put("param1", "2023-01-01");

        ParameterValidationResult result = validator.validateParameters(mockVersion, parameterValues);

        // Should be valid
        assertTrue(result.isValid());
        assertEquals(1, result.getValidatedValues().size());
        assertEquals(java.time.LocalDate.of(2023, 1, 1), result.getValidatedValues().get("param1"));
    }

    @Test
    public void testValidateParameters_DateType_Invalid() {
        // Setup a version with date parameter
        Set<PromptParameter> parameters = new HashSet<>();
        parameters.add(createParameter("param1", "Parameter 1", ParameterType.DATE, false, null, null));

        when(mockVersion.getParameters()).thenReturn(parameters);

        // Test with invalid date
        Map<String, Object> parameterValues = new HashMap<>();
        parameterValues.put("param1", "not-a-date");

        ParameterValidationResult result = validator.validateParameters(mockVersion, parameterValues);

        // Should not validate successfully
        assertTrue(result.isValid()); // It should be valid overall since the parameter is optional
        assertEquals(1, result.getIssues().size());
        assertEquals(ParameterValidationResult.ValidationSeverity.ERROR, result.getIssues().get(0).getSeverity());
        assertEquals(0, result.getValidatedValues().size());
    }

    @Test
    public void testValidateParameters_DateTimeType_Valid() {
        // Setup a version with datetime parameter
        Set<PromptParameter> parameters = new HashSet<>();
        parameters.add(createParameter("param1", "Parameter 1", ParameterType.DATETIME, false, null, null));

        when(mockVersion.getParameters()).thenReturn(parameters);

        // Test with valid datetime
        Map<String, Object> parameterValues = new HashMap<>();
        parameterValues.put("param1", "2023-01-01T12:00:00");

        ParameterValidationResult result = validator.validateParameters(mockVersion, parameterValues);

        // Should be valid
        assertTrue(result.isValid());
        assertEquals(1, result.getValidatedValues().size());
        assertEquals(java.time.LocalDateTime.of(2023, 1, 1, 12, 0, 0), result.getValidatedValues().get("param1"));
    }

    @Test
    public void testValidateParameters_DateTimeType_Invalid() {
        // Setup a version with datetime parameter
        Set<PromptParameter> parameters = new HashSet<>();
        parameters.add(createParameter("param1", "Parameter 1", ParameterType.DATETIME, false, null, null));

        when(mockVersion.getParameters()).thenReturn(parameters);

        // Test with invalid datetime
        Map<String, Object> parameterValues = new HashMap<>();
        parameterValues.put("param1", "not-a-datetime");

        ParameterValidationResult result = validator.validateParameters(mockVersion, parameterValues);

        // Should not validate successfully
        assertTrue(result.isValid()); // It should be valid overall since the parameter is optional
        assertEquals(1, result.getIssues().size());
        assertEquals(ParameterValidationResult.ValidationSeverity.ERROR, result.getIssues().get(0).getSeverity());
        assertEquals(0, result.getValidatedValues().size());
    }

    @Test
    public void testValidateParameters_ArrayType_Valid() {
        // Setup a version with array parameter
        Set<PromptParameter> parameters = new HashSet<>();
        parameters.add(createParameter("param1", "Parameter 1", ParameterType.ARRAY, false, null, null));

        when(mockVersion.getParameters()).thenReturn(parameters);

        // Test with valid array
        Map<String, Object> parameterValues = new HashMap<>();
        parameterValues.put("param1", "[1, 2, 3]");

        ParameterValidationResult result = validator.validateParameters(mockVersion, parameterValues);

        // Should be valid
        assertTrue(result.isValid());
        assertEquals(1, result.getValidatedValues().size());
        assertEquals("[1, 2, 3]", result.getValidatedValues().get("param1"));
    }

    @Test
    public void testValidateParameters_ArrayType_Invalid() {
        // Setup a version with array parameter
        Set<PromptParameter> parameters = new HashSet<>();
        parameters.add(createParameter("param1", "Parameter 1", ParameterType.ARRAY, false, null, null));

        when(mockVersion.getParameters()).thenReturn(parameters);

        // Test with invalid array
        Map<String, Object> parameterValues = new HashMap<>();
        parameterValues.put("param1", "not-an-array");

        ParameterValidationResult result = validator.validateParameters(mockVersion, parameterValues);

        // Should not validate successfully
        assertTrue(result.isValid()); // It should be valid overall since the parameter is optional
        assertEquals(1, result.getIssues().size());
        assertEquals(ParameterValidationResult.ValidationSeverity.ERROR, result.getIssues().get(0).getSeverity());
        assertEquals(0, result.getValidatedValues().size());
    }

    @Test
    public void testValidateParameters_ObjectType_Valid() {
        // Setup a version with object parameter
        Set<PromptParameter> parameters = new HashSet<>();
        parameters.add(createParameter("param1", "Parameter 1", ParameterType.OBJECT, false, null, null));

        when(mockVersion.getParameters()).thenReturn(parameters);

        // Test with valid object
        Map<String, Object> parameterValues = new HashMap<>();
        parameterValues.put("param1", "{\"key\": \"value\"}");

        ParameterValidationResult result = validator.validateParameters(mockVersion, parameterValues);

        // Should be valid
        assertTrue(result.isValid());
        assertEquals(1, result.getValidatedValues().size());
        assertEquals("{\"key\": \"value\"}", result.getValidatedValues().get("param1"));
    }

    @Test
    public void testValidateParameters_ObjectType_Invalid() {
        // Setup a version with object parameter
        Set<PromptParameter> parameters = new HashSet<>();
        parameters.add(createParameter("param1", "Parameter 1", ParameterType.OBJECT, false, null, null));

        when(mockVersion.getParameters()).thenReturn(parameters);

        // Test with invalid object
        Map<String, Object> parameterValues = new HashMap<>();
        parameterValues.put("param1", "not-an-object");

        ParameterValidationResult result = validator.validateParameters(mockVersion, parameterValues);

        // Should not validate successfully
        assertTrue(result.isValid()); // It should be valid overall since the parameter is optional
        assertEquals(1, result.getIssues().size());
        assertEquals(ParameterValidationResult.ValidationSeverity.ERROR, result.getIssues().get(0).getSeverity());
        assertEquals(0, result.getValidatedValues().size());
    }

    @Test
    public void testValidateParameters_ValidationPattern_Valid() {
        // Setup a version with a parameter with validation pattern
        Set<PromptParameter> parameters = new HashSet<>();
        parameters.add(createParameter("param1", "Parameter 1", ParameterType.STRING, false, null, "^[a-z]+$"));

        when(mockVersion.getParameters()).thenReturn(parameters);

        // Test with valid value matching pattern
        Map<String, Object> parameterValues = new HashMap<>();
        parameterValues.put("param1", "abcdef");

        ParameterValidationResult result = validator.validateParameters(mockVersion, parameterValues);

        // Should be valid
        assertTrue(result.isValid());
        assertEquals(1, result.getValidatedValues().size());
        assertEquals("abcdef", result.getValidatedValues().get("param1"));
    }

    @Test
    public void testValidateParameters_ValidationPattern_Invalid() {
        // Setup a version with a parameter with validation pattern
        Set<PromptParameter> parameters = new HashSet<>();
        parameters.add(createParameter("param1", "Parameter 1", ParameterType.STRING, false, null, "^[a-z]+$"));

        when(mockVersion.getParameters()).thenReturn(parameters);

        // Test with invalid value not matching pattern
        Map<String, Object> parameterValues = new HashMap<>();
        parameterValues.put("param1", "ABC123");

        ParameterValidationResult result = validator.validateParameters(mockVersion, parameterValues);

        // Should not validate successfully
        assertTrue(result.isValid()); // It should be valid overall since the parameter is optional
        assertEquals(1, result.getIssues().size());
        assertEquals(ParameterValidationResult.ValidationSeverity.ERROR, result.getIssues().get(0).getSeverity());
        assertEquals(0, result.getValidatedValues().size());
    }

    @Test
    public void testValidateParameters_DefaultValue() {
        // Setup a version with a parameter with default value
        Set<PromptParameter> parameters = new HashSet<>();
        parameters.add(createParameter("param1", "Parameter 1", ParameterType.STRING, false, "default", null));

        when(mockVersion.getParameters()).thenReturn(parameters);

        // Test without providing the parameter
        Map<String, Object> parameterValues = new HashMap<>();

        ParameterValidationResult result = validator.validateParameters(mockVersion, parameterValues);

        // Should be valid with default value
        assertTrue(result.isValid());
        assertEquals(1, result.getValidatedValues().size());
        assertEquals("default", result.getValidatedValues().get("param1"));
    }

    @Test
    public void testValidateParameters_MultipleParameters() {
        // Setup a version with multiple parameters of different types
        Set<PromptParameter> parameters = new HashSet<>();
        parameters.add(createParameter("string", "String Parameter", ParameterType.STRING, true, null, null));
        parameters.add(createParameter("number", "Number Parameter", ParameterType.NUMBER, true, null, null));
        parameters.add(createParameter("boolean", "Boolean Parameter", ParameterType.BOOLEAN, false, "true", null));
        parameters.add(createParameter("date", "Date Parameter", ParameterType.DATE, false, null, null));

        when(mockVersion.getParameters()).thenReturn(parameters);

        // Test with valid values for required parameters
        Map<String, Object> parameterValues = new HashMap<>();
        parameterValues.put("string", "test");
        parameterValues.put("number", "123");
        parameterValues.put("date", "2023-01-01");

        ParameterValidationResult result = validator.validateParameters(mockVersion, parameterValues);

        // Should be valid
        assertTrue(result.isValid());
        assertEquals(4, result.getValidatedValues().size());
        assertEquals("test", result.getValidatedValues().get("string"));
        assertEquals(123, result.getValidatedValues().get("number"));
        assertEquals(true, result.getValidatedValues().get("boolean")); // from default value
        assertEquals(java.time.LocalDate.of(2023, 1, 1), result.getValidatedValues().get("date"));
    }

    /**
     * Helper method to create a parameter for testing
     */
    private PromptParameter createParameter(String name, String description, ParameterType type,
                                            boolean required, String defaultValue, String validationPattern) {
        PromptParameter parameter = mock(PromptParameter.class);

        when(parameter.getName()).thenReturn(name);
        when(parameter.getDescription()).thenReturn(description);
        when(parameter.getParameterType()).thenReturn(type);
        when(parameter.isRequired()).thenReturn(required);
        when(parameter.getDefaultValue()).thenReturn(defaultValue);
        when(parameter.getValidationPattern()).thenReturn(validationPattern);

        // Mock the validateValue method based on the parameter type and pattern
        when(parameter.validateValue(any())).thenAnswer(invocation -> {
            Object value = invocation.getArgument(0);

            if (value == null) {
                return !required;
            }

            String strValue = value.toString();

            if (validationPattern != null && type == ParameterType.STRING) {
                if (!strValue.matches(validationPattern)) {
                    return false;
                }
            }

            switch (type) {
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
                        java.time.LocalDate.parse(strValue);
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                case DATETIME:
                    try {
                        java.time.LocalDateTime.parse(strValue);
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                case ARRAY:
                    return strValue.startsWith("[") && strValue.endsWith("]");
                case OBJECT:
                    return strValue.startsWith("{") && strValue.endsWith("}");
                default:
                    return true;
            }
        });

        // Mock the getTypedDefaultValue method
        when(parameter.getTypedDefaultValue()).thenAnswer(invocation -> {
            if (defaultValue == null) {
                return null;
            }

            switch (type) {
                case NUMBER:
                    if (defaultValue.contains(".")) {
                        return Double.parseDouble(defaultValue);
                    } else {
                        return Integer.parseInt(defaultValue);
                    }
                case BOOLEAN:
                    String lowerValue = defaultValue.toLowerCase();
                    return lowerValue.equals("true") || lowerValue.equals("1") || lowerValue.equals("yes");
                case DATE:
                    return java.time.LocalDate.parse(defaultValue);
                case DATETIME:
                    return java.time.LocalDateTime.parse(defaultValue);
                default:
                    return defaultValue;
            }
        });

        // Mock the convertValue method
        when(parameter.convertValue(anyString())).thenAnswer(invocation -> {
            String value = invocation.getArgument(0);

            if (value == null) {
                return null;
            }

            switch (type) {
                case NUMBER:
                    if (value.contains(".")) {
                        return Double.parseDouble(value);
                    } else {
                        return Integer.parseInt(value);
                    }
                case BOOLEAN:
                    String lowerValue = value.toLowerCase();
                    return lowerValue.equals("true") || lowerValue.equals("1") || lowerValue.equals("yes");
                case DATE:
                    return java.time.LocalDate.parse(value);
                case DATETIME:
                    return java.time.LocalDateTime.parse(value);
                default:
                    return value;
            }
        });

        return parameter;
    }
}