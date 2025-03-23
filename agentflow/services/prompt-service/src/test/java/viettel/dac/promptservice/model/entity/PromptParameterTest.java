package viettel.dac.promptservice.model.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import viettel.dac.promptservice.model.enums.ParameterType;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class PromptParameterTest {

    private PromptParameter parameter;

    @BeforeEach
    void setUp() {
        parameter = new PromptParameter();
        parameter.setId("param-1");
        parameter.setName("testParam");
    }

    @Nested
    @DisplayName("Type Validation Tests")
    class TypeValidationTests {

        @Test
        @DisplayName("Should validate STRING parameters correctly")
        void shouldValidateStringParametersCorrectly() {
            // Arrange
            parameter.setParameterType(ParameterType.STRING);

            // Act & Assert
            assertTrue(parameter.validateValue("Regular string"));
            assertTrue(parameter.validateValue("123"));
            assertTrue(parameter.validateValue(""));
        }

        @Test
        @DisplayName("Should validate NUMBER parameters correctly")
        void shouldValidateNumberParametersCorrectly() {
            // Arrange
            parameter.setParameterType(ParameterType.NUMBER);

            // Act & Assert
            assertTrue(parameter.validateValue("123"));
            assertTrue(parameter.validateValue("-123"));
            assertTrue(parameter.validateValue("123.45"));
            assertTrue(parameter.validateValue("-123.45"));

            assertFalse(parameter.validateValue("not a number"));
            assertFalse(parameter.validateValue("123abc"));
        }

        @Test
        @DisplayName("Should validate BOOLEAN parameters correctly")
        void shouldValidateBooleanParametersCorrectly() {
            // Arrange
            parameter.setParameterType(ParameterType.BOOLEAN);

            // Act & Assert
            assertTrue(parameter.validateValue("true"));
            assertTrue(parameter.validateValue("false"));
            assertTrue(parameter.validateValue("TRUE"));
            assertTrue(parameter.validateValue("FALSE"));
            assertTrue(parameter.validateValue("1"));
            assertTrue(parameter.validateValue("0"));
            assertTrue(parameter.validateValue("yes"));
            assertTrue(parameter.validateValue("no"));

            assertFalse(parameter.validateValue("not boolean"));
            assertFalse(parameter.validateValue("2"));
        }

        @Test
        @DisplayName("Should validate DATE parameters correctly")
        void shouldValidateDateParametersCorrectly() {
            // Arrange
            parameter.setParameterType(ParameterType.DATE);

            // Act & Assert
            assertTrue(parameter.validateValue("2023-07-15"));

            assertFalse(parameter.validateValue("invalid date"));
            assertFalse(parameter.validateValue("07/15/2023"));
        }

        @Test
        @DisplayName("Should validate ARRAY parameters correctly")
        void shouldValidateArrayParametersCorrectly() {
            // Arrange
            parameter.setParameterType(ParameterType.ARRAY);

            // Act & Assert
            assertTrue(parameter.validateValue("[1, 2, 3]"));
            assertTrue(parameter.validateValue("[]"));

            assertFalse(parameter.validateValue("not an array"));
            assertFalse(parameter.validateValue("{key: value}"));
        }
    }

    @Nested
    @DisplayName("Required Parameter Tests")
    class RequiredParameterTests {

        @Test
        @DisplayName("Should validate required parameters correctly")
        void shouldValidateRequiredParametersCorrectly() {
            // Arrange
            parameter.setParameterType(ParameterType.STRING);
            parameter.setRequired(true);

            // Act & Assert
            assertTrue(parameter.validateValue("value"));
            assertFalse(parameter.validateValue(null));
            assertFalse(parameter.validateValue(""));
        }

        @Test
        @DisplayName("Should validate optional parameters correctly")
        void shouldValidateOptionalParametersCorrectly() {
            // Arrange
            parameter.setParameterType(ParameterType.STRING);
            parameter.setRequired(false);

            // Act & Assert
            assertTrue(parameter.validateValue("value"));
            assertTrue(parameter.validateValue(null));
            assertTrue(parameter.validateValue(""));
        }
    }

    @Nested
    @DisplayName("Pattern Validation Tests")
    class PatternValidationTests {

        @Test
        @DisplayName("Should validate against pattern correctly")
        void shouldValidateAgainstPatternCorrectly() {
            // Arrange
            parameter.setParameterType(ParameterType.STRING);
            parameter.setValidationPattern("^[a-z]+$");

            // Act & Assert
            assertTrue(parameter.validateValue("abc"));
            assertFalse(parameter.validateValue("123"));
            assertFalse(parameter.validateValue("abc123"));
        }

        @Test
        @DisplayName("Should ignore validation if pattern is null")
        void shouldIgnoreValidationIfPatternIsNull() {
            // Arrange
            parameter.setParameterType(ParameterType.STRING);
            parameter.setValidationPattern(null);

            // Act & Assert
            assertTrue(parameter.validateValue("any value"));
        }
    }

    @Nested
    @DisplayName("Value Conversion Tests")
    class ValueConversionTests {

        @Test
        @DisplayName("Should convert NUMBER values correctly")
        void shouldConvertNumberValuesCorrectly() {
            // Arrange
            parameter.setParameterType(ParameterType.NUMBER);

            // Act & Assert
            assertEquals(123, parameter.convertValue("123"));
            assertEquals(123.45, parameter.convertValue("123.45"));

            assertThrows(IllegalArgumentException.class, () -> parameter.convertValue("not a number"));
        }

        @Test
        @DisplayName("Should convert BOOLEAN values correctly")
        void shouldConvertBooleanValuesCorrectly() {
            // Arrange
            parameter.setParameterType(ParameterType.BOOLEAN);

            // Act & Assert
            assertEquals(Boolean.TRUE, parameter.convertValue("true"));
            assertEquals(Boolean.TRUE, parameter.convertValue("TRUE"));
            assertEquals(Boolean.TRUE, parameter.convertValue("1"));
            assertEquals(Boolean.TRUE, parameter.convertValue("yes"));

            assertEquals(Boolean.FALSE, parameter.convertValue("false"));
            assertEquals(Boolean.FALSE, parameter.convertValue("FALSE"));
            assertEquals(Boolean.FALSE, parameter.convertValue("0"));
            assertEquals(Boolean.FALSE, parameter.convertValue("no"));
        }

        @Test
        @DisplayName("Should convert DATE and DATETIME values correctly")
        void shouldConvertDateAndDatetimeValuesCorrectly() {
            // Arrange - Date
            parameter.setParameterType(ParameterType.DATE);

            // Act & Assert - Date
            Object dateResult = parameter.convertValue("2023-07-15");
            assertTrue(dateResult instanceof LocalDate);
            assertEquals(LocalDate.of(2023, 7, 15), dateResult);

            // Arrange - DateTime
            parameter.setParameterType(ParameterType.DATETIME);

            // Act & Assert - DateTime
            Object dateTimeResult = parameter.convertValue("2023-07-15T14:30:00");
            assertTrue(dateTimeResult instanceof LocalDateTime);
            assertEquals(LocalDateTime.of(2023, 7, 15, 14, 30, 0), dateTimeResult);
        }
    }

    @Nested
    @DisplayName("Default Value Tests")
    class DefaultValueTests {

        @Test
        @DisplayName("Should get typed default values correctly")
        void shouldGetTypedDefaultValuesCorrectly() {
            // Arrange & Act & Assert - String
            parameter.setParameterType(ParameterType.STRING);
            parameter.setDefaultValue("default");
            assertEquals("default", parameter.getTypedDefaultValue());

            // Arrange & Act & Assert - Number
            parameter.setParameterType(ParameterType.NUMBER);
            parameter.setDefaultValue("123.45");
            assertEquals(123.45, parameter.getTypedDefaultValue());

            // Arrange & Act & Assert - Boolean
            parameter.setParameterType(ParameterType.BOOLEAN);
            parameter.setDefaultValue("true");
            assertEquals(Boolean.TRUE, parameter.getTypedDefaultValue());
        }

        @Test
        @DisplayName("Should return null for null or empty default value")
        void shouldReturnNullForNullOrEmptyDefaultValue() {
            // Arrange & Act & Assert - Null
            parameter.setParameterType(ParameterType.STRING);
            parameter.setDefaultValue(null);
            assertNull(parameter.getTypedDefaultValue());

            // Arrange & Act & Assert - Empty
            parameter.setDefaultValue("");
            assertNull(parameter.getTypedDefaultValue());
        }
    }
}