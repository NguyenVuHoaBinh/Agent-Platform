package viettel.dac.promptservice.model.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import viettel.dac.promptservice.model.enums.VersionStatus;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PromptVersionTest {

    private PromptVersion promptVersion;
    private PromptTemplate template;

    @BeforeEach
    void setUp() {
        template = new PromptTemplate();
        template.setId("template-1");
        template.setName("Test Template");

        promptVersion = new PromptVersion();
        promptVersion.setId("version-1");
        promptVersion.setTemplate(template);
        promptVersion.setVersionNumber("1.2.3");
        promptVersion.setContent("This is a test prompt with {{parameter1}} and {{parameter2}}");
        promptVersion.setStatus(VersionStatus.DRAFT);

        PromptParameter parameter1 = new PromptParameter();
        parameter1.setName("parameter1");
        parameter1.setRequired(true);

        PromptParameter parameter2 = new PromptParameter();
        parameter2.setName("parameter2");
        parameter2.setRequired(false);

        promptVersion.addParameter(parameter1);
        promptVersion.addParameter(parameter2);
    }

    @Nested
    @DisplayName("Parameter Substitution Tests")
    class ParameterSubstitutionTests {

        @Test
        @DisplayName("Should substitute parameters correctly")
        void shouldSubstituteParametersCorrectly() {
            // Arrange
            Map<String, Object> params = new HashMap<>();
            params.put("parameter1", "value1");
            params.put("parameter2", "value2");

            // Act
            String result = promptVersion.applyParameters(params);

            // Assert
            assertEquals("This is a test prompt with value1 and value2", result);
        }

        @Test
        @DisplayName("Should throw exception for missing required parameters")
        void shouldThrowExceptionForMissingRequiredParameters() {
            // Arrange
            Map<String, Object> params = new HashMap<>();
            params.put("parameter2", "value2");

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> promptVersion.applyParameters(params));
            assertTrue(exception.getMessage().contains("parameter1"));
        }

        @Test
        @DisplayName("Should ignore extra parameters")
        void shouldIgnoreExtraParameters() {
            // Arrange
            Map<String, Object> params = new HashMap<>();
            params.put("parameter1", "value1");
            params.put("parameter2", "value2");
            params.put("extraParam", "extra");

            // Act
            String result = promptVersion.applyParameters(params);

            // Assert
            assertEquals("This is a test prompt with value1 and value2", result);
        }

        @Test
        @DisplayName("Should handle null for optional parameters")
        void shouldHandleNullForOptionalParameters() {
            // Arrange
            Map<String, Object> params = new HashMap<>();
            params.put("parameter1", "value1");
            params.put("parameter2", null);

            // Act
            String result = promptVersion.applyParameters(params);

            // Assert
            assertEquals("This is a test prompt with value1 and null", result);
        }
    }

    @Nested
    @DisplayName("Version Number Tests")
    class VersionNumberTests {

        @Test
        @DisplayName("Should extract version components correctly")
        void shouldExtractVersionComponentsCorrectly() {
            // Arrange
            promptVersion.setVersionNumber("2.3.4");

            // Act & Assert
            assertEquals(2, promptVersion.getMajorVersion());
            assertEquals(3, promptVersion.getMinorVersion());
            assertEquals(4, promptVersion.getPatchVersion());
        }

        @Test
        @DisplayName("Should increment version numbers correctly")
        void shouldIncrementVersionNumbersCorrectly() {
            // Arrange
            promptVersion.setVersionNumber("2.3.4");

            // Act & Assert
            assertEquals("3.0.0", promptVersion.incrementMajorVersion());
            assertEquals("2.4.0", promptVersion.incrementMinorVersion());
            assertEquals("2.3.5", promptVersion.incrementPatchVersion());
        }

        @Test
        @DisplayName("Should compare versions correctly")
        void shouldCompareVersionsCorrectly() {
            // Arrange
            promptVersion.setVersionNumber("1.2.3");

            PromptVersion higherMajor = new PromptVersion();
            higherMajor.setVersionNumber("2.0.0");

            PromptVersion higherMinor = new PromptVersion();
            higherMinor.setVersionNumber("1.3.0");

            PromptVersion higherPatch = new PromptVersion();
            higherPatch.setVersionNumber("1.2.4");

            PromptVersion same = new PromptVersion();
            same.setVersionNumber("1.2.3");

            // Act & Assert
            assertTrue(promptVersion.compareVersion(higherMajor) < 0);
            assertTrue(promptVersion.compareVersion(higherMinor) < 0);
            assertTrue(promptVersion.compareVersion(higherPatch) < 0);
            assertEquals(0, promptVersion.compareVersion(same));
        }
    }

    @Nested
    @DisplayName("Status Transition Tests")
    class StatusTransitionTests {

        @Test
        @DisplayName("Should validate allowed status transitions")
        void shouldValidateAllowedStatusTransitions() {
            // Draft to Review is allowed
            promptVersion.setStatus(VersionStatus.DRAFT);
            assertTrue(promptVersion.canTransitionTo(VersionStatus.REVIEW));

            // Review to Approved is allowed
            promptVersion.setStatus(VersionStatus.REVIEW);
            assertTrue(promptVersion.canTransitionTo(VersionStatus.APPROVED));

            // Approved to Published is allowed
            promptVersion.setStatus(VersionStatus.APPROVED);
            assertTrue(promptVersion.canTransitionTo(VersionStatus.PUBLISHED));

            // Published to Deprecated is allowed
            promptVersion.setStatus(VersionStatus.PUBLISHED);
            assertTrue(promptVersion.canTransitionTo(VersionStatus.DEPRECATED));

            // Deprecated to Archived is allowed
            promptVersion.setStatus(VersionStatus.DEPRECATED);
            assertTrue(promptVersion.canTransitionTo(VersionStatus.ARCHIVED));
        }

        @Test
        @DisplayName("Should validate disallowed status transitions")
        void shouldValidateDisallowedStatusTransitions() {
            // Draft to Published is not allowed
            promptVersion.setStatus(VersionStatus.DRAFT);
            assertFalse(promptVersion.canTransitionTo(VersionStatus.PUBLISHED));

            // Published to Draft is not allowed
            promptVersion.setStatus(VersionStatus.PUBLISHED);
            assertFalse(promptVersion.canTransitionTo(VersionStatus.DRAFT));

            // Archived to any status is not allowed
            promptVersion.setStatus(VersionStatus.ARCHIVED);
            assertFalse(promptVersion.canTransitionTo(VersionStatus.DRAFT));
            assertFalse(promptVersion.canTransitionTo(VersionStatus.REVIEW));
            assertFalse(promptVersion.canTransitionTo(VersionStatus.APPROVED));
            assertFalse(promptVersion.canTransitionTo(VersionStatus.PUBLISHED));
            assertFalse(promptVersion.canTransitionTo(VersionStatus.DEPRECATED));
        }
    }

    @Nested
    @DisplayName("Parameter Extraction Tests")
    class ParameterExtractionTests {

        @Test
        @DisplayName("Should extract parameters from content")
        void shouldExtractParametersFromContent() {
            // Arrange
            String content = "Text with {{param1}} and {{param2}} and {{param3}}";
            promptVersion.setContent(content);

            // Act
            Set<String> extractedParams = promptVersion.extractParametersFromContent();

            // Assert
            assertEquals(3, extractedParams.size());
            assertTrue(extractedParams.contains("param1"));
            assertTrue(extractedParams.contains("param2"));
            assertTrue(extractedParams.contains("param3"));
        }

        @Test
        @DisplayName("Should extract duplicate parameters only once")
        void shouldExtractDuplicateParametersOnlyOnce() {
            // Arrange
            String content = "Text with {{param}} and more {{param}}";
            promptVersion.setContent(content);

            // Act
            Set<String> extractedParams = promptVersion.extractParametersFromContent();

            // Assert
            assertEquals(1, extractedParams.size());
            assertTrue(extractedParams.contains("param"));
        }

        @Test
        @DisplayName("Should return empty set for content with no parameters")
        void shouldReturnEmptySetForContentWithNoParameters() {
            // Arrange
            String content = "Text with no parameters";
            promptVersion.setContent(content);

            // Act
            Set<String> extractedParams = promptVersion.extractParametersFromContent();

            // Assert
            assertTrue(extractedParams.isEmpty());
        }
    }
}