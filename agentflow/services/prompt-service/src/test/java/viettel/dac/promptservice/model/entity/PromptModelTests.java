package viettel.dac.promptservice.model.entity;

import org.junit.jupiter.api.Test;
import viettel.dac.promptservice.model.enums.ParameterType;
import viettel.dac.promptservice.model.enums.VersionStatus;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PromptModelTests {

    @Test
    void testPromptTemplateVersionManagement() {
        // Create template
        PromptTemplate template = PromptTemplate.builder()
                .name("Test Template")
                .description("Test Description")
                .projectId("test-project")
                .createdBy("test-user")
                .category("General")
                .build();

        // Create version
        PromptVersion version = PromptVersion.builder()
                .versionNumber("1.0.0")
                .content("This is a {{test}} prompt")
                .createdBy("test-user")
                .status(VersionStatus.DRAFT)
                .build();

        // Test relationship management
        template.addVersion(version);
        assertEquals(1, template.getVersions().size());
        assertSame(template, version.getTemplate());

        template.removeVersion(version);
        assertEquals(0, template.getVersions().size());
        assertNull(version.getTemplate());

        // Test latest published version
        PromptVersion draftVersion = PromptVersion.builder()
                .versionNumber("1.0.0")
                .content("Draft content")
                .createdBy("test-user")
                .status(VersionStatus.DRAFT)
                .build();

        PromptVersion publishedVersion = PromptVersion.builder()
                .versionNumber("1.0.0")
                .content("Published content")
                .createdBy("test-user")
                .status(VersionStatus.PUBLISHED)
                .build();

        template.addVersion(draftVersion);
        template.addVersion(publishedVersion);

        assertTrue(template.hasPublishedVersion());
        assertTrue(template.getLatestPublishedVersion().isPresent());
        assertEquals(publishedVersion, template.getLatestPublishedVersion().get());
    }

    @Test
    void testPromptVersionSemver() {
        PromptVersion version = new PromptVersion();
        version.setVersionNumber("1.2.3");

        assertEquals(1, version.getMajorVersion());
        assertEquals(2, version.getMinorVersion());
        assertEquals(3, version.getPatchVersion());

        assertEquals("2.0.0", version.incrementMajorVersion());
        assertEquals("1.3.0", version.incrementMinorVersion());
        assertEquals("1.2.4", version.incrementPatchVersion());

        // Test version comparison
        PromptVersion v1 = new PromptVersion();
        v1.setVersionNumber("1.0.0");

        PromptVersion v2 = new PromptVersion();
        v2.setVersionNumber("1.1.0");

        PromptVersion v3 = new PromptVersion();
        v3.setVersionNumber("2.0.0");

        assertTrue(v1.compareVersion(v2) < 0); // 1.0.0 < 1.1.0
        assertTrue(v2.compareVersion(v1) > 0); // 1.1.0 > 1.0.0
        assertTrue(v2.compareVersion(v3) < 0); // 1.1.0 < 2.0.0
        assertEquals(0, v1.compareVersion(v1)); // 1.0.0 = 1.0.0
    }

    @Test
    void testPromptVersionParameterExtraction() {
        PromptVersion version = new PromptVersion();
        version.setContent("This is a {{param1}} template with {{param2}} and {{param3}}");

        Set<String> params = version.extractParametersFromContent();
        assertEquals(3, params.size());
        assertTrue(params.contains("param1"));
        assertTrue(params.contains("param2"));
        assertTrue(params.contains("param3"));
    }

    @Test
    void testPromptVersionParameterSubstitution() {
        PromptVersion version = new PromptVersion();
        version.setContent("Hello {{name}}, welcome to {{service}}!");

        PromptParameter param1 = PromptParameter.builder()
                .name("name")
                .parameterType(ParameterType.STRING)
                .required(true)
                .build();

        PromptParameter param2 = PromptParameter.builder()
                .name("service")
                .parameterType(ParameterType.STRING)
                .required(true)
                .build();

        version.addParameter(param1);
        version.addParameter(param2);

        Map<String, Object> values = new HashMap<>();
        values.put("name", "John");
        values.put("service", "PromptService");

        String result = version.applyParameters(values);
        assertEquals("Hello John, welcome to PromptService!", result);

        // Test missing required parameter
        Map<String, Object> incompleteValues = new HashMap<>();
        incompleteValues.put("name", "John");

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            version.applyParameters(incompleteValues);
        });

        assertTrue(exception.getMessage().contains("Missing required parameters"));
    }

    @Test
    void testPromptVersionStatusTransitions() {
        PromptVersion version = new PromptVersion();
        version.setStatus(VersionStatus.DRAFT);

        assertTrue(version.canTransitionTo(VersionStatus.REVIEW));
        assertFalse(version.canTransitionTo(VersionStatus.PUBLISHED));

        version.setStatus(VersionStatus.REVIEW);
        assertTrue(version.canTransitionTo(VersionStatus.APPROVED));
        assertTrue(version.canTransitionTo(VersionStatus.DRAFT));
        assertFalse(version.canTransitionTo(VersionStatus.PUBLISHED));

        version.setStatus(VersionStatus.APPROVED);
        assertTrue(version.canTransitionTo(VersionStatus.PUBLISHED));
        assertTrue(version.canTransitionTo(VersionStatus.REVIEW));

        version.setStatus(VersionStatus.PUBLISHED);
        assertTrue(version.canTransitionTo(VersionStatus.DEPRECATED));
        assertFalse(version.canTransitionTo(VersionStatus.DRAFT));

        version.setStatus(VersionStatus.DEPRECATED);
        assertTrue(version.canTransitionTo(VersionStatus.ARCHIVED));
        assertFalse(version.canTransitionTo(VersionStatus.PUBLISHED));

        version.setStatus(VersionStatus.ARCHIVED);
        assertFalse(version.canTransitionTo(VersionStatus.DRAFT));
        assertFalse(version.canTransitionTo(VersionStatus.PUBLISHED));
    }

    @Test
    void testPromptParameterValidation() {
        // String parameter
        PromptParameter stringParam = PromptParameter.builder()
                .name("name")
                .parameterType(ParameterType.STRING)
                .validationPattern("[A-Za-z]+")
                .required(true)
                .build();

        assertTrue(stringParam.validateValue("John"));
        assertFalse(stringParam.validateValue("John123"));
        assertFalse(stringParam.validateValue(null));

        // Number parameter
        PromptParameter numberParam = PromptParameter.builder()
                .name("age")
                .parameterType(ParameterType.NUMBER)
                .required(false)
                .build();

        assertTrue(numberParam.validateValue(25));
        assertTrue(numberParam.validateValue("25.5"));
        assertFalse(numberParam.validateValue("twenty-five"));
        assertTrue(numberParam.validateValue(null)); // Not required

        // Boolean parameter
        PromptParameter booleanParam = PromptParameter.builder()
                .name("active")
                .parameterType(ParameterType.BOOLEAN)
                .required(true)
                .build();

        assertTrue(booleanParam.validateValue(true));
        assertTrue(booleanParam.validateValue("true"));
        assertTrue(booleanParam.validateValue("FALSE"));
        assertTrue(booleanParam.validateValue("1"));
        assertTrue(booleanParam.validateValue("yes"));
        assertFalse(booleanParam.validateValue("maybe"));
    }

    @Test
    void testPromptParameterTypeConversion() {
        // Number conversion
        PromptParameter numberParam = PromptParameter.builder()
                .name("amount")
                .parameterType(ParameterType.NUMBER)
                .build();

        assertEquals(42, numberParam.convertValue("42"));
        assertEquals(42.5, numberParam.convertValue("42.5"));

        // Boolean conversion
        PromptParameter booleanParam = PromptParameter.builder()
                .name("active")
                .parameterType(ParameterType.BOOLEAN)
                .build();

        assertEquals(Boolean.TRUE, booleanParam.convertValue("true"));
        assertEquals(Boolean.TRUE, booleanParam.convertValue("YES"));
        assertEquals(Boolean.TRUE, booleanParam.convertValue("1"));
        assertEquals(Boolean.FALSE, booleanParam.convertValue("false"));
        assertEquals(Boolean.FALSE, booleanParam.convertValue("no"));
        assertEquals(Boolean.FALSE, booleanParam.convertValue("0"));

        // Default value handling
        PromptParameter paramWithDefault = PromptParameter.builder()
                .name("score")
                .parameterType(ParameterType.NUMBER)
                .defaultValue("100")
                .build();

        assertEquals(100, paramWithDefault.getTypedDefaultValue());
    }
}