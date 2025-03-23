package viettel.dac.promptservice.service.elasticsearch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import viettel.dac.promptservice.model.document.PromptExecutionDocument;
import viettel.dac.promptservice.model.document.PromptTemplateDocument;
import viettel.dac.promptservice.model.document.PromptVersionDocument;
import viettel.dac.promptservice.model.entity.PromptExecution;
import viettel.dac.promptservice.model.entity.PromptParameter;
import viettel.dac.promptservice.model.entity.PromptTemplate;
import viettel.dac.promptservice.model.entity.PromptVersion;
import viettel.dac.promptservice.model.enums.ExecutionStatus;
import viettel.dac.promptservice.model.enums.ParameterType;
import viettel.dac.promptservice.model.enums.VersionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class DocumentConversionServiceTest {

    @InjectMocks
    private DocumentConversionService conversionService;

    // Test data
    private PromptTemplate testTemplate;
    private PromptVersion testVersion;
    private PromptExecution testExecution;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();

        // Setup test template
        testTemplate = PromptTemplate.builder()
                .id("template-123")
                .name("Test Template")
                .description("Test Description")
                .category("Test Category")
                .projectId("project-123")
                .createdBy("user-123")
                .createdAt(now)
                .updatedAt(now)
                .build();

        // Setup test version
        testVersion = PromptVersion.builder()
                .id("version-123")
                .template(testTemplate)
                .versionNumber("1.0.0")
                .content("This is a test prompt with {{parameter}}")
                .status(VersionStatus.PUBLISHED)
                .createdBy("user-123")
                .createdAt(now)
                .updatedAt(now)
                .versionNumber("1.0.0")
                .build();

        // Add parameter to version
        PromptParameter parameter = PromptParameter.builder()
                .id("param-123")
                .version(testVersion)
                .name("parameter")
                .description("Test parameter")
                .parameterType(ParameterType.STRING)
                .required(true)
                .build();
        testVersion.addParameter(parameter);

        // Setup test execution
        testExecution = PromptExecution.builder()
                .id("execution-123")
                .version(testVersion)
                .providerId("openai")
                .modelId("gpt-3.5-turbo")
                .inputParameters(Collections.singletonMap("parameter", "test value"))
                .rawResponse("This is a generated response")
                .tokenCount(30)
                .inputTokens(10)
                .outputTokens(20)
                .cost(BigDecimal.valueOf(0.002))
                .responseTimeMs(500L)
                .executedAt(now)
                .executedBy("user-123")
                .status(ExecutionStatus.SUCCESS)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    @Test
    @DisplayName("Should correctly parse version components from version number")
    void shouldCorrectlyParseVersionComponents() {
        // Arrange
        testVersion.setVersionNumber("2.3.4");

        // Act
        PromptVersionDocument document = conversionService.convertVersionToDocument(testVersion);

        // Assert
        assertEquals("2.3.4", document.getVersionNumber());
        assertEquals(2, document.getMajorVersion());
        assertEquals(3, document.getMinorVersion());
        assertEquals(4, document.getPatchVersion());
    }

    @Test
    @DisplayName("Should convert template entity to document")
    void shouldConvertTemplateToDocument() {
        // Arrange
        testTemplate.addVersion(testVersion);

        // Act
        PromptTemplateDocument document = conversionService.convertTemplateToDocument(testTemplate);

        // Assert
        assertNotNull(document);
        assertEquals(testTemplate.getId(), document.getId());
        assertEquals(testTemplate.getName(), document.getName());
        assertEquals(testTemplate.getDescription(), document.getDescription());
        assertEquals(testTemplate.getCategory(), document.getCategory());
        assertEquals(testTemplate.getProjectId(), document.getProjectId());
        assertEquals(testTemplate.getCreatedBy(), document.getCreatedBy());
        assertEquals(testTemplate.getCreatedAt(), document.getCreatedAt());
        assertEquals(testTemplate.getUpdatedAt(), document.getUpdatedAt());

        // Check additional search fields
        assertTrue(document.isHasPublishedVersion());
        assertEquals(1, document.getVersionCount());

        // Check keywords for faceted search
        assertTrue(document.getKeywords().containsKey("project"));
        assertTrue(document.getKeywords().containsKey("category"));
        assertTrue(document.getKeywords().containsKey("creator"));
        assertEquals(testTemplate.getProjectId(), document.getKeywords().get("project"));
        assertEquals(testTemplate.getCategory(), document.getKeywords().get("category"));
        assertEquals(testTemplate.getCreatedBy(), document.getKeywords().get("creator"));
    }

    @Test
    @DisplayName("Should convert version entity to document")
    void shouldConvertVersionToDocument() {
        // Act
        PromptVersionDocument document = conversionService.convertVersionToDocument(testVersion);

        // Assert
        assertNotNull(document);
        assertEquals(testVersion.getId(), document.getId());
        assertEquals(testVersion.getTemplate().getId(), document.getTemplateId());
        assertEquals(testVersion.getTemplate().getName(), document.getTemplateName());
        assertEquals(testVersion.getVersionNumber(), document.getVersionNumber());
        assertEquals(testVersion.getContent(), document.getContent());
        assertEquals(testVersion.getStatus().name(), document.getStatus());
        assertEquals(testVersion.getCreatedBy(), document.getCreatedBy());
        assertEquals(testVersion.getCreatedAt(), document.getCreatedAt());
        assertEquals(testVersion.getUpdatedAt(), document.getUpdatedAt());
        assertEquals(testVersion.getMajorVersion(), document.getMajorVersion());
        assertEquals(testVersion.getMinorVersion(), document.getMinorVersion());
        assertEquals(testVersion.getPatchVersion(), document.getPatchVersion());

        // Check parameters
        assertNotNull(document.getParameters());
        assertEquals(1, document.getParameters().size());
        assertEquals("parameter", document.getParameters().get(0).getName());

        // Check parameter names
        assertNotNull(document.getParameterNames());
        assertEquals(1, document.getParameterNames().size());
        assertTrue(document.getParameterNames().contains("parameter"));

        // Check content tokens
        assertNotNull(document.getContentTokens());
        assertFalse(document.getContentTokens().isEmpty());
    }

    @Test
    @DisplayName("Should convert execution entity to document")
    void shouldConvertExecutionToDocument() {
        // Act
        PromptExecutionDocument document = conversionService.convertExecutionToDocument(testExecution);

        // Assert
        assertNotNull(document);
        assertEquals(testExecution.getId(), document.getId());
        assertEquals(testExecution.getVersion().getId(), document.getVersionId());
        assertEquals(testExecution.getVersion().getTemplate().getId(), document.getTemplateId());
        assertEquals(testExecution.getProviderId(), document.getProviderId());
        assertEquals(testExecution.getModelId(), document.getModelId());
        assertEquals(testExecution.getInputParameters(), document.getInputParameters());
        assertEquals(testExecution.getTokenCount(), document.getTokenCount());
        assertEquals(testExecution.getInputTokens(), document.getInputTokens());
        assertEquals(testExecution.getOutputTokens(), document.getOutputTokens());
        assertEquals(testExecution.getCost(), document.getCost());
        assertEquals(testExecution.getResponseTimeMs(), document.getResponseTimeMs());
        assertEquals(testExecution.getExecutedAt(), document.getExecutedAt());
        assertEquals(testExecution.getExecutedBy(), document.getExecutedBy());
        assertEquals(testExecution.getStatus().name(), document.getStatus());
        assertEquals(testExecution.getCreatedAt(), document.getCreatedAt());
        assertEquals(testExecution.getUpdatedAt(), document.getUpdatedAt());

        // Check metrics for aggregations
        assertTrue(document.getSuccessful());
        assertEquals(testExecution.getExecutedAt().getYear(), document.getYear());
        assertEquals(testExecution.getExecutedAt().getMonthValue(), document.getMonth());
        assertEquals(testExecution.getExecutedAt().getDayOfMonth(), document.getDay());
    }

    @Test
    @DisplayName("Should handle null input when converting template")
    void shouldHandleNullInputWhenConvertingTemplate() {
        // Act
        PromptTemplateDocument document = conversionService.convertTemplateToDocument(null);

        // Assert
        assertNull(document);
    }

    @Test
    @DisplayName("Should handle null input when converting version")
    void shouldHandleNullInputWhenConvertingVersion() {
        // Act
        PromptVersionDocument document = conversionService.convertVersionToDocument(null);

        // Assert
        assertNull(document);
    }

    @Test
    @DisplayName("Should handle null input when converting execution")
    void shouldHandleNullInputWhenConvertingExecution() {
        // Act
        PromptExecutionDocument document = conversionService.convertExecutionToDocument(null);

        // Assert
        assertNull(document);
    }

    @Test
    @DisplayName("Should convert template with published version flag")
    void shouldConvertTemplateWithPublishedVersionFlag() {
        // Arrange
        testTemplate.addVersion(testVersion);  // testVersion is PUBLISHED

        // Act
        PromptTemplateDocument document = conversionService.convertTemplateToDocument(testTemplate);

        // Assert
        assertTrue(document.isHasPublishedVersion());
    }

    @Test
    @DisplayName("Should convert template without published version flag")
    void shouldConvertTemplateWithoutPublishedVersionFlag() {
        // Arrange
        PromptVersion draftVersion = PromptVersion.builder()
                .id("draft-version")
                .template(testTemplate)
                .status(VersionStatus.DRAFT)
                .build();

        testTemplate.getVersions().clear();
        testTemplate.addVersion(draftVersion);

        // Act
        PromptTemplateDocument document = conversionService.convertTemplateToDocument(testTemplate);

        // Assert
        assertFalse(document.isHasPublishedVersion());
    }

    @Test
    @DisplayName("Should convert version with parameters")
    void shouldConvertVersionWithParameters() {
        // Arrange
        // Add another parameter
        PromptParameter additionalParam = PromptParameter.builder()
                .id("param-456")
                .version(testVersion)
                .name("second_parameter")
                .parameterType(ParameterType.NUMBER)
                .required(false)
                .build();
        testVersion.addParameter(additionalParam);

        // Act
        PromptVersionDocument document = conversionService.convertVersionToDocument(testVersion);

        // Assert
        assertNotNull(document.getParameters());
        assertEquals(2, document.getParameters().size());

        // Check parameter names set
        Set<String> paramNames = document.getParameterNames();
        assertNotNull(paramNames);
        assertEquals(2, paramNames.size());
        assertTrue(paramNames.contains("parameter"));
        assertTrue(paramNames.contains("second_parameter"));
    }

    @Test
    @DisplayName("Should tokenize content for improved search")
    void shouldTokenizeContentForImprovedSearch() {
        // Arrange
        String content = "This is a test. Content with punctuation, and multiple words!";
        testVersion.setContent(content);

        // Act
        PromptVersionDocument document = conversionService.convertVersionToDocument(testVersion);

        // Assert
        List<String> tokens = document.getContentTokens();
        assertNotNull(tokens);
        assertFalse(tokens.isEmpty());

        // Check tokenization (exact tokens might vary based on implementation)
        assertTrue(tokens.contains("test"));
        assertTrue(tokens.contains("content"));
        assertTrue(tokens.contains("with"));
        assertTrue(tokens.contains("punctuation"));
        assertTrue(tokens.contains("multiple"));
        assertTrue(tokens.contains("words"));
    }

    @Test
    @DisplayName("Should convert lists of entities")
    void shouldConvertListsOfEntities() {
        // Arrange
        PromptTemplate template2 = PromptTemplate.builder()
                .id("template-456")
                .name("Second Template")
                .build();

        List<PromptTemplate> templates = Arrays.asList(testTemplate, template2);

        PromptVersion version2 = PromptVersion.builder()
                .id("version-456")
                .template(template2)
                .status(VersionStatus.DRAFT)
                .versionNumber("1.0.0")
                .build();

        List<PromptVersion> versions = Arrays.asList(testVersion, version2);

        PromptExecution execution2 = PromptExecution.builder()
                .id("execution-456")
                .version(version2)
                .status(ExecutionStatus.SUCCESS)
                .executedAt(now)
                .build();

        List<PromptExecution> executions = Arrays.asList(testExecution, execution2);

        // Act
        List<PromptTemplateDocument> templateDocs = conversionService.convertTemplatesToDocuments(templates);
        List<PromptVersionDocument> versionDocs = conversionService.convertVersionsToDocuments(versions);
        List<PromptExecutionDocument> executionDocs = conversionService.convertExecutionsToDocuments(executions);

        // Assert
        assertNotNull(templateDocs);
        assertEquals(2, templateDocs.size());
        assertEquals("template-123", templateDocs.get(0).getId());
        assertEquals("template-456", templateDocs.get(1).getId());

        assertNotNull(versionDocs);
        assertEquals(2, versionDocs.size());
        assertEquals("version-123", versionDocs.get(0).getId());
        assertEquals("version-456", versionDocs.get(1).getId());

        assertNotNull(executionDocs);
        assertEquals(2, executionDocs.size());
        assertEquals("execution-123", executionDocs.get(0).getId());
        assertEquals("execution-456", executionDocs.get(1).getId());
    }

    @Test
    @DisplayName("Should handle version without parameters")
    void shouldHandleVersionWithoutParameters() {
        // Arrange
        PromptVersion versionWithoutParams = PromptVersion.builder()
                .id("no-params-version")
                .template(testTemplate)
                .versionNumber("1.1.0")
                .content("No parameters here")
                .status(VersionStatus.DRAFT)
                .build();

        // Act
        PromptVersionDocument document = conversionService.convertVersionToDocument(versionWithoutParams);

        // Assert
        assertNotNull(document);
        assertTrue(document.getParameters() == null || document.getParameters().isEmpty());
        assertTrue(document.getParameterNames() == null || document.getParameterNames().isEmpty());
    }

    @Test
    @DisplayName("Should handle empty content when tokenizing")
    void shouldHandleEmptyContentWhenTokenizing() {
        // Arrange
        testVersion.setContent("");

        // Act
        PromptVersionDocument document = conversionService.convertVersionToDocument(testVersion);

        // Assert
        List<String> tokens = document.getContentTokens();
        assertNotNull(tokens);
        assertTrue(tokens.isEmpty());
    }

    @Test
    @DisplayName("Should handle null content when tokenizing")
    void shouldHandleNullContentWhenTokenizing() {
        // Arrange
        testVersion.setContent(null);

        // Act
        PromptVersionDocument document = conversionService.convertVersionToDocument(testVersion);

        // Assert
        List<String> tokens = document.getContentTokens();
        assertNotNull(tokens);
        assertTrue(tokens.isEmpty());
    }
}