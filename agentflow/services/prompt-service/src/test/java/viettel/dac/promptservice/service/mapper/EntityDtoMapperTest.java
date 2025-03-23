package viettel.dac.promptservice.service.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import viettel.dac.promptservice.dto.request.PromptParameterRequest;
import viettel.dac.promptservice.dto.request.PromptTemplateRequest;
import viettel.dac.promptservice.dto.request.PromptVersionRequest;
import viettel.dac.promptservice.dto.response.*;
import viettel.dac.promptservice.model.entity.PromptExecution;
import viettel.dac.promptservice.model.entity.PromptParameter;
import viettel.dac.promptservice.model.entity.PromptTemplate;
import viettel.dac.promptservice.model.entity.PromptVersion;
import viettel.dac.promptservice.model.enums.ExecutionStatus;
import viettel.dac.promptservice.model.enums.ParameterType;
import viettel.dac.promptservice.model.enums.VersionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class EntityDtoMapperTest {

    private EntityDtoMapper mapper;

    // Test data
    private final String TEMPLATE_ID = "template-123";
    private final String VERSION_ID = "version-123";
    private final String PARAMETER_ID = "parameter-123";
    private final String EXECUTION_ID = "execution-123";
    private final LocalDateTime now = LocalDateTime.now();

    private PromptTemplate testTemplate;
    private PromptVersion testVersion;
    private PromptParameter testParameter;
    private PromptExecution testExecution;

    @BeforeEach
    void setUp() {
        mapper = new EntityDtoMapper();

        // Setup test template
        testTemplate = PromptTemplate.builder()
                .id(TEMPLATE_ID)
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
                .id(VERSION_ID)
                .template(testTemplate)
                .versionNumber("1.0.0")
                .content("This is a test prompt with {{parameter}}")
                .systemPrompt("You are a helpful assistant.")
                .status(VersionStatus.PUBLISHED)
                .createdBy("user-123")
                .createdAt(now)
                .updatedAt(now)
                .build();

        // Setup test parameter
        testParameter = PromptParameter.builder()
                .id(PARAMETER_ID)
                .version(testVersion)
                .name("parameter")
                .description("Test parameter")
                .parameterType(ParameterType.STRING)
                .defaultValue("default value")
                .required(true)
                .validationPattern("[a-z]+")
                .build();

        // Add parameter to version
        testVersion.addParameter(testParameter);

        // Add version to template
        testTemplate.addVersion(testVersion);

        // Setup test execution
        testExecution = PromptExecution.builder()
                .id(EXECUTION_ID)
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
                .build();
    }

    @Test
    @DisplayName("Should convert template to response DTO")
    void shouldConvertTemplateToResponseDto() {
        // Act
        PromptTemplateResponse response = mapper.toTemplateResponse(testTemplate);

        // Assert
        assertNotNull(response);
        assertEquals(TEMPLATE_ID, response.getId());
        assertEquals("Test Template", response.getName());
        assertEquals("Test Description", response.getDescription());
        assertEquals("Test Category", response.getCategory());
        assertEquals("project-123", response.getProjectId());
        assertEquals("user-123", response.getCreatedBy());
        assertEquals(now, response.getCreatedAt());
        assertEquals(now, response.getUpdatedAt());
        assertTrue(response.isHasPublishedVersion());
        assertEquals(1, response.getVersionCount());
    }

    @Test
    @DisplayName("Should convert template with versions to response")
    void shouldConvertTemplateWithVersionsToResponse() {
        // Act
        PromptTemplateResponse response = mapper.toTemplateResponseWithVersions(testTemplate);

        // Assert
        assertNotNull(response);
        assertEquals(TEMPLATE_ID, response.getId());

        // Check versions
        assertNotNull(response.getVersions());
        assertEquals(1, response.getVersions().size());

        PromptVersionSummaryResponse versionSummary = response.getVersions().get(0);
        assertEquals(VERSION_ID, versionSummary.getId());
        assertEquals("1.0.0", versionSummary.getVersionNumber());
        assertEquals(VersionStatus.PUBLISHED, versionSummary.getStatus());
        assertEquals(1, versionSummary.getParameterCount());
    }

    @Test
    @DisplayName("Should convert version to summary response")
    void shouldConvertVersionToSummaryResponse() {
        // Act
        PromptVersionSummaryResponse response = mapper.toVersionSummaryResponse(testVersion);

        // Assert
        assertNotNull(response);
        assertEquals(VERSION_ID, response.getId());
        assertEquals("1.0.0", response.getVersionNumber());
        assertEquals(VersionStatus.PUBLISHED, response.getStatus());
        assertEquals("user-123", response.getCreatedBy());
        assertEquals(now, response.getCreatedAt());
        assertEquals(1, response.getParameterCount());
    }

    @Test
    @DisplayName("Should convert version to full response")
    void shouldConvertVersionToFullResponse() {
        // Act
        PromptVersionResponse response = mapper.toVersionResponse(testVersion);

        // Assert
        assertNotNull(response);
        assertEquals(VERSION_ID, response.getId());
        assertEquals(TEMPLATE_ID, response.getTemplateId());
        assertEquals("Test Template", response.getTemplateName());
        assertEquals("1.0.0", response.getVersionNumber());
        assertEquals("This is a test prompt with {{parameter}}", response.getContent());
        assertEquals("You are a helpful assistant.", response.getSystemPrompt());
        assertEquals(VersionStatus.PUBLISHED, response.getStatus());

        // Check parameters
        assertNotNull(response.getParameters());
        assertEquals(1, response.getParameters().size());

        PromptParameterResponse paramResponse = response.getParameters().get(0);
        assertEquals(PARAMETER_ID, paramResponse.getId());
        assertEquals("parameter", paramResponse.getName());
    }

    @Test
    @DisplayName("Should convert version with parent to response")
    void shouldConvertVersionWithParentToResponse() {
        // Arrange
        PromptVersion parentVersion = PromptVersion.builder()
                .id("parent-version")
                .template(testTemplate)
                .versionNumber("0.1.0")
                .build();

        testVersion.setParentVersion(parentVersion);

        // Act
        PromptVersionResponse response = mapper.toVersionResponse(testVersion);

        // Assert
        assertNotNull(response);
        assertEquals(VERSION_ID, response.getId());
        assertEquals("parent-version", response.getParentVersionId());
    }

    @Test
    @DisplayName("Should convert parameter to response")
    void shouldConvertParameterToResponse() {
        // Act
        PromptParameterResponse response = mapper.toParameterResponse(testParameter);

        // Assert
        assertNotNull(response);
        assertEquals(PARAMETER_ID, response.getId());
        assertEquals("parameter", response.getName());
        assertEquals("Test parameter", response.getDescription());
        assertEquals(ParameterType.STRING, response.getParameterType());
        assertEquals("default value", response.getDefaultValue());
        assertTrue(response.isRequired());
        assertEquals("[a-z]+", response.getValidationPattern());
    }

    @Test
    @DisplayName("Should convert execution to response")
    void shouldConvertExecutionToResponse() {
        // Act
        PromptExecutionResponse response = mapper.toExecutionResponse(testExecution);

        // Assert
        assertNotNull(response);
        assertEquals(EXECUTION_ID, response.getId());
        assertEquals(VERSION_ID, response.getVersionId());
        assertEquals(TEMPLATE_ID, response.getTemplateId());
        assertEquals("openai", response.getProviderId());
        assertEquals("gpt-3.5-turbo", response.getModelId());
        assertEquals(Collections.singletonMap("parameter", "test value"), response.getInputParameters());
        assertEquals("This is a generated response", response.getResponse());
        assertEquals(30, response.getTokenCount());
        assertEquals(10, response.getInputTokens());
        assertEquals(20, response.getOutputTokens());
        assertEquals(BigDecimal.valueOf(0.002), response.getCost());
        assertEquals(500L, response.getResponseTimeMs());
        assertEquals(now, response.getExecutedAt());
        assertEquals("user-123", response.getExecutedBy());
        assertEquals(ExecutionStatus.SUCCESS, response.getStatus());
    }

    @Test
    @DisplayName("Should convert template request to entity")
    void shouldConvertTemplateRequestToEntity() {
        // Arrange
        PromptTemplateRequest request = PromptTemplateRequest.builder()
                .name("New Template")
                .description("New Description")
                .category("New Category")
                .projectId("project-456")
                .build();

        // Act
        PromptTemplate entity = mapper.toTemplateEntity(request);

        // Assert
        assertNotNull(entity);
        assertEquals("New Template", entity.getName());
        assertEquals("New Description", entity.getDescription());
        assertEquals("New Category", entity.getCategory());
        assertEquals("project-456", entity.getProjectId());
    }

    @Test
    @DisplayName("Should update template from request")
    void shouldUpdateTemplateFromRequest() {
        // Arrange
        PromptTemplateRequest request = PromptTemplateRequest.builder()
                .name("Updated Template")
                .description("Updated Description")
                .category("Updated Category")
                .projectId("project-updated")
                .build();

        PromptTemplate templateToUpdate = PromptTemplate.builder()
                .id(TEMPLATE_ID)
                .name("Original Name")
                .description("Original Description")
                .category("Original Category")
                .projectId("project-original")
                .build();

        // Act
        mapper.updateTemplateFromRequest(templateToUpdate, request);

        // Assert
        assertEquals("Updated Template", templateToUpdate.getName());
        assertEquals("Updated Description", templateToUpdate.getDescription());
        assertEquals("Updated Category", templateToUpdate.getCategory());
        assertEquals("project-original", templateToUpdate.getProjectId());  // projectId should not be updated
    }

    @Test
    @DisplayName("Should convert version request to entity")
    void shouldConvertVersionRequestToEntity() {
        // Arrange
        PromptVersionRequest request = PromptVersionRequest.builder()
                .versionNumber("2.0.0")
                .content("New content")
                .systemPrompt("New system prompt")
                .build();

        // Act
        PromptVersion entity = mapper.toVersionEntity(request);

        // Assert
        assertNotNull(entity);
        assertEquals("2.0.0", entity.getVersionNumber());
        assertEquals("New content", entity.getContent());
        assertEquals("New system prompt", entity.getSystemPrompt());
    }

    @Test
    @DisplayName("Should convert parameter request to entity")
    void shouldConvertParameterRequestToEntity() {
        // Arrange
        PromptParameterRequest request = PromptParameterRequest.builder()
                .name("new_param")
                .description("New parameter")
                .parameterType(ParameterType.NUMBER)
                .defaultValue("42")
                .required(false)
                .validationPattern("[0-9]+")
                .build();

        // Act
        PromptParameter entity = mapper.toParameterEntity(request);

        // Assert
        assertNotNull(entity);
        assertEquals("new_param", entity.getName());
        assertEquals("New parameter", entity.getDescription());
        assertEquals(ParameterType.NUMBER, entity.getParameterType());
        assertEquals("42", entity.getDefaultValue());
        assertFalse(entity.isRequired());
        assertEquals("[0-9]+", entity.getValidationPattern());
    }

    @Test
    @DisplayName("Should convert page to page response")
    void shouldConvertPageToPageResponse() {
        // Arrange
        List<PromptTemplate> templates = new ArrayList<>();
        templates.add(testTemplate);

        PromptTemplate template2 = PromptTemplate.builder()
                .id("template-456")
                .name("Second Template")
                .build();
        templates.add(template2);

        Page<PromptTemplate> page = new PageImpl<>(templates, PageRequest.of(0, 10), 2);
        Function<PromptTemplate, PromptTemplateResponse> mapperFunction = mapper::toTemplateResponse;

        // Act
        PageResponse<PromptTemplateResponse> response = mapper.toPageResponse(page, mapperFunction);

        // Assert
        assertNotNull(response);
        assertEquals(2, response.getTotalElements());
        assertEquals(1, response.getTotalPages());
        assertEquals(0, response.getPage());
        assertEquals(10, response.getSize());
        assertTrue(response.isFirst());
        assertTrue(response.isLast());

        // Check content
        assertNotNull(response.getContent());
        assertEquals(2, response.getContent().size());
        assertEquals(TEMPLATE_ID, response.getContent().get(0).getId());
        assertEquals("template-456", response.getContent().get(1).getId());
    }

    @Test
    @DisplayName("Should handle template with null fields")
    void shouldHandleTemplateWithNullFields() {
        // Arrange
        PromptTemplate templateWithNulls = PromptTemplate.builder()
                .id(TEMPLATE_ID)
                .name("Test Template")
                .build();

        // Act
        PromptTemplateResponse response = mapper.toTemplateResponse(templateWithNulls);

        // Assert
        assertNotNull(response);
        assertEquals(TEMPLATE_ID, response.getId());
        assertEquals("Test Template", response.getName());
        assertNull(response.getDescription());
        assertNull(response.getCategory());
        assertNull(response.getProjectId());
        assertNull(response.getCreatedBy());
        assertNull(response.getCreatedAt());
        assertNull(response.getUpdatedAt());
        assertFalse(response.isHasPublishedVersion());
        assertEquals(0, response.getVersionCount());
    }

    @Test
    @DisplayName("Should handle version without parent")
    void shouldHandleVersionWithoutParent() {
        // Arrange
        PromptVersion versionWithoutParent = PromptVersion.builder()
                .id(VERSION_ID)
                .template(testTemplate)
                .versionNumber("1.0.0")
                .build();

        // Act
        PromptVersionResponse response = mapper.toVersionResponse(versionWithoutParent);

        // Assert
        assertNotNull(response);
        assertNull(response.getParentVersionId());
    }

    @Test
    @DisplayName("Should handle version without parameters")
    void shouldHandleVersionWithoutParameters() {
        // Arrange
        PromptVersion versionWithoutParams = PromptVersion.builder()
                .id(VERSION_ID)
                .template(testTemplate)
                .versionNumber("1.0.0")
                .build();

        // Act
        PromptVersionResponse response = mapper.toVersionResponse(versionWithoutParams);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getParameters());
        assertTrue(response.getParameters().isEmpty());
    }
}