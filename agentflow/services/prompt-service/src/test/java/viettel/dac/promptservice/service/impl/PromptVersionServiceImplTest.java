package viettel.dac.promptservice.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import viettel.dac.promptservice.dto.request.PromptParameterRequest;
import viettel.dac.promptservice.dto.request.PromptVersionRequest;
import viettel.dac.promptservice.dto.response.VersionComparisonResult;
import viettel.dac.promptservice.exception.ResourceNotFoundException;
import viettel.dac.promptservice.exception.ValidationException;
import viettel.dac.promptservice.model.entity.PromptParameter;
import viettel.dac.promptservice.model.entity.PromptTemplate;
import viettel.dac.promptservice.model.entity.PromptVersion;
import viettel.dac.promptservice.model.entity.VersionAuditEntry;
import viettel.dac.promptservice.model.enums.ParameterType;
import viettel.dac.promptservice.model.enums.VersionStatus;
import viettel.dac.promptservice.repository.jpa.PromptParameterRepository;
import viettel.dac.promptservice.repository.jpa.PromptTemplateRepository;
import viettel.dac.promptservice.repository.jpa.PromptVersionRepository;
import viettel.dac.promptservice.repository.jpa.VersionAuditRepository;
import viettel.dac.promptservice.security.SecurityUtils;
import viettel.dac.promptservice.service.event.VersionStatusChangeEvent;
import viettel.dac.promptservice.util.DiffUtility;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PromptVersionServiceImplTest {

    @Mock
    private PromptVersionRepository versionRepository;

    @Mock
    private PromptTemplateRepository templateRepository;

    @Mock
    private PromptParameterRepository parameterRepository;

    @Mock
    private VersionAuditRepository auditRepository;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private DiffUtility diffUtility;

    @InjectMocks
    private PromptVersionServiceImpl versionService;

    // Test data
    private final String TEMPLATE_ID = "template-123";
    private final String VERSION_ID = "version-123";
    private final String PARENT_VERSION_ID = "parent-version-123";
    private final String USER_ID = "user-123";
    private final String VERSION_NUMBER = "1.0.0";
    private final String VERSION_CONTENT = "This is a test prompt with {{parameter}}";

    private PromptTemplate testTemplate;
    private PromptVersion testVersion;
    private PromptVersion parentVersion;
    private PromptVersionRequest versionRequest;

    @BeforeEach
    void setUp() {
        // Setup test template
        testTemplate = PromptTemplate.builder()
                .id(TEMPLATE_ID)
                .name("Test Template")
                .description("Test Description")
                .projectId("project-123")
                .createdBy(USER_ID)
                .build();

        // Setup parent version
        parentVersion = PromptVersion.builder()
                .id(PARENT_VERSION_ID)
                .template(testTemplate)
                .versionNumber("0.1.0")
                .content("Parent content with {{parameter}}")
                .status(VersionStatus.PUBLISHED)
                .createdBy(USER_ID)
                .build();

        // Setup test version
        testVersion = PromptVersion.builder()
                .id(VERSION_ID)
                .template(testTemplate)
                .versionNumber(VERSION_NUMBER)
                .content(VERSION_CONTENT)
                .status(VersionStatus.DRAFT)
                .createdBy(USER_ID)
                .parentVersion(parentVersion)
                .build();

        // Add test parameter to version
        PromptParameter parameter = PromptParameter.builder()
                .id("param-123")
                .version(testVersion)
                .name("parameter")
                .description("Test parameter")
                .parameterType(ParameterType.STRING)
                .required(true)
                .build();
        testVersion.addParameter(parameter);

        // Setup version request
        versionRequest = PromptVersionRequest.builder()
                .templateId(TEMPLATE_ID)
                .versionNumber(VERSION_NUMBER)
                .content(VERSION_CONTENT)
                .parentVersionId(PARENT_VERSION_ID)
                .build();

        // Stub security utils as lenient (not every test needs this)
        lenient().when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(USER_ID));

        // Stub the lookup for parent version to avoid ResourceNotFoundException in tests where a parent is expected
        lenient().when(versionRepository.findById(PARENT_VERSION_ID)).thenReturn(Optional.of(parentVersion));
    }

    @Test
    @DisplayName("Should create a new version successfully")
    void shouldCreateVersionSuccessfully() {
        // Arrange
        when(templateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(testTemplate));
        when(versionRepository.findByTemplateIdAndVersionNumber(TEMPLATE_ID, VERSION_NUMBER)).thenReturn(Optional.empty());
        when(versionRepository.save(any(PromptVersion.class))).thenAnswer(i -> i.getArgument(0));

        // Add parameter to request
        PromptParameterRequest paramRequest = PromptParameterRequest.builder()
                .name("parameter")
                .description("Test parameter")
                .parameterType(ParameterType.STRING)
                .required(true)
                .build();
        versionRequest.setParameters(Collections.singletonList(paramRequest));

        // Act
        PromptVersion result = versionService.createVersion(versionRequest);

        // Assert
        assertNotNull(result);
        assertEquals(TEMPLATE_ID, result.getTemplate().getId());
        assertEquals(VERSION_NUMBER, result.getVersionNumber());
        assertEquals(VERSION_CONTENT, result.getContent());
        assertEquals(VersionStatus.DRAFT, result.getStatus());
        assertEquals(USER_ID, result.getCreatedBy());

        // Verify parameter creation
        assertEquals(1, result.getParameters().size());
        PromptParameter param = result.getParameters().iterator().next();
        assertEquals("parameter", param.getName());
        assertEquals(ParameterType.STRING, param.getParameterType());
        assertTrue(param.isRequired());

        // Verify repository interactions
        verify(versionRepository, times(2)).save(any(PromptVersion.class));
        verify(auditRepository).save(any(VersionAuditEntry.class));
    }

    @Test
    @DisplayName("Should throw exception when creating version with invalid version number")
    void shouldThrowExceptionForInvalidVersionNumber() {
        // Arrange
        versionRequest.setVersionNumber("invalid");

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class,
                () -> versionService.createVersion(versionRequest));
        assertTrue(exception.getMessage().contains("Version validation failed"));
    }

    @Test
    @DisplayName("Should throw exception when creating version with duplicate version number")
    void shouldThrowExceptionForDuplicateVersionNumber() {
        // Arrange
        when(templateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(testTemplate));
        // Stub duplicate version check to return an existing version
        when(versionRepository.findByTemplateIdAndVersionNumber(TEMPLATE_ID, VERSION_NUMBER))
                .thenReturn(Optional.of(testVersion));
        // Also stub parent version lookup so it doesn't fail early
        when(versionRepository.findById(PARENT_VERSION_ID)).thenReturn(Optional.of(parentVersion));

        // Act & Assert
        ValidationException exception = assertThrows(ValidationException.class,
                () -> versionService.createVersion(versionRequest));
        assertTrue(exception.getMessage().contains("Version number already exists"));
    }

    @Test
    @DisplayName("Should get version by ID")
    void shouldGetVersionById() {
        // Arrange
        when(versionRepository.findById(VERSION_ID)).thenReturn(Optional.of(testVersion));

        // Act
        Optional<PromptVersion> result = versionService.getVersionById(VERSION_ID);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(VERSION_ID, result.get().getId());
    }

    @Test
    @DisplayName("Should return empty optional when version not found")
    void shouldReturnEmptyOptionalWhenVersionNotFound() {
        // Arrange
        when(versionRepository.findById("non-existent")).thenReturn(Optional.empty());

        // Act
        Optional<PromptVersion> result = versionService.getVersionById("non-existent");

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should get versions by template")
    void shouldGetVersionsByTemplate() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<PromptVersion> versionsPage = new PageImpl<>(Collections.singletonList(testVersion));
        when(templateRepository.existsById(TEMPLATE_ID)).thenReturn(true);
        when(versionRepository.findByTemplateId(eq(TEMPLATE_ID), any(Pageable.class)))
                .thenReturn(versionsPage);

        // Act
        Page<PromptVersion> result = versionService.getVersionsByTemplate(TEMPLATE_ID, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(testVersion.getId(), result.getContent().get(0).getId());
    }

    @Test
    @DisplayName("Should throw exception when getting versions for non-existent template")
    void shouldThrowExceptionWhenTemplateNotFound() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        when(templateRepository.existsById("non-existent")).thenReturn(false);

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> versionService.getVersionsByTemplate("non-existent", pageable));
    }

    @Test
    @DisplayName("Should update version status successfully")
    void shouldUpdateVersionStatusSuccessfully() {
        // Arrange
        when(versionRepository.findById(VERSION_ID)).thenReturn(Optional.of(testVersion));
        when(versionRepository.save(any(PromptVersion.class))).thenReturn(testVersion);

        // Act
        PromptVersion result = versionService.updateVersionStatus(VERSION_ID, VersionStatus.REVIEW);

        // Assert
        assertEquals(VersionStatus.REVIEW, result.getStatus());
        verify(auditRepository).save(any(VersionAuditEntry.class));
        verify(eventPublisher).publishEvent(any(VersionStatusChangeEvent.class));
    }

    @Test
    @DisplayName("Should throw exception for invalid status transition")
    void shouldThrowExceptionForInvalidStatusTransition() {
        // Arrange
        testVersion.setStatus(VersionStatus.ARCHIVED);
        when(versionRepository.findById(VERSION_ID)).thenReturn(Optional.of(testVersion));

        // Act & Assert
        assertThrows(ValidationException.class,
                () -> versionService.updateVersionStatus(VERSION_ID, VersionStatus.PUBLISHED));
    }

    @Test
    @DisplayName("Should deprecate existing published versions when publishing new version")
    void shouldDeprecateExistingPublishedVersionsWhenPublishing() {
        // Arrange
        PromptVersion existingPublishedVersion = PromptVersion.builder()
                .id("published-version")
                .template(testTemplate)
                .versionNumber("0.9.0")
                .status(VersionStatus.PUBLISHED)
                .build();

        when(versionRepository.findById(VERSION_ID)).thenReturn(Optional.of(testVersion));
        when(versionRepository.findByTemplateIdAndStatus(eq(TEMPLATE_ID), eq(VersionStatus.PUBLISHED), any()))
                .thenReturn(new PageImpl<>(Collections.singletonList(existingPublishedVersion)));
        when(versionRepository.save(any(PromptVersion.class)))
                .thenAnswer(i -> i.getArgument(0));

        // Act
        PromptVersion result = versionService.updateVersionStatus(VERSION_ID, VersionStatus.PUBLISHED);

        // Assert
        assertEquals(VersionStatus.PUBLISHED, result.getStatus());

        // Verify existing published version was deprecated
        verify(versionRepository, times(2)).save(any(PromptVersion.class));

        // Verify we saved the audit entries for both versions
        verify(auditRepository, times(2)).save(any(VersionAuditEntry.class));

        // Verify events were published
        verify(eventPublisher, times(2)).publishEvent(any(VersionStatusChangeEvent.class));
    }

    @Test
    @DisplayName("Should create branch from existing version")
    void shouldCreateBranchFromExistingVersion() {
        // Arrange
        PromptVersionRequest branchRequest = PromptVersionRequest.builder()
                .versionNumber("1.1.0")
                .content("Branched content")
                .build();

        when(versionRepository.findByIdWithParameters(PARENT_VERSION_ID))
                .thenReturn(Optional.of(parentVersion));
        when(versionRepository.findByTemplateIdAndVersionNumber(eq(TEMPLATE_ID), eq("1.1.0")))
                .thenReturn(Optional.empty());
        when(versionRepository.save(any(PromptVersion.class)))
                .thenAnswer(i -> i.getArgument(0));

        // Act
        PromptVersion result = versionService.createBranch(PARENT_VERSION_ID, branchRequest);

        // Assert
        assertNotNull(result);
        assertEquals("1.1.0", result.getVersionNumber());
        assertEquals("Branched content", result.getContent());
        assertEquals(VersionStatus.DRAFT, result.getStatus());
        assertEquals(parentVersion.getId(), result.getParentVersion().getId());

        // Verify audit entry was created
        verify(auditRepository).save(any(VersionAuditEntry.class));
    }

    @Test
    @DisplayName("Should throw exception when creating branch from non-existent version")
    void shouldThrowExceptionWhenBranchSourceNotFound() {
        // Arrange
        PromptVersionRequest branchRequest = PromptVersionRequest.builder()
                .versionNumber("1.1.0")
                .content("Branched content")
                .build();

        when(versionRepository.findByIdWithParameters("non-existent"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> versionService.createBranch("non-existent", branchRequest));
    }

    @Test
    @DisplayName("Should compare versions and return differences")
    void shouldCompareVersionsAndReturnDifferences() {
        // Arrange
        PromptVersion version1 = testVersion;
        PromptVersion version2 = PromptVersion.builder()
                .id("version-456")
                .template(testTemplate)
                .versionNumber("1.1.0")
                .content("Updated content with {{parameter}} and {{new_param}}")
                .status(VersionStatus.DRAFT)
                .createdBy(USER_ID)
                .build();

        // Add parameters to version2
        PromptParameter existingParam = PromptParameter.builder()
                .id("param-123")
                .version(version2)
                .name("parameter")
                .description("Updated description")
                .parameterType(ParameterType.STRING)
                .required(true)
                .build();

        PromptParameter newParam = PromptParameter.builder()
                .id("param-456")
                .version(version2)
                .name("new_param")
                .description("New parameter")
                .parameterType(ParameterType.NUMBER)
                .required(false)
                .build();

        version2.addParameter(existingParam);
        version2.addParameter(newParam);

        when(versionRepository.findByIdWithParameters(version1.getId()))
                .thenReturn(Optional.of(version1));
        when(versionRepository.findByIdWithParameters(version2.getId()))
                .thenReturn(Optional.of(version2));

        // Set up diff utility to return some diffs
        List<VersionComparisonResult.TextDiff> textDiffs = Collections.singletonList(
                VersionComparisonResult.TextDiff.builder()
                        .type(VersionComparisonResult.DiffType.ADDITION)
                        .text(" and {{new_param}}")
                        .position(28)
                        .build());

        when(diffUtility.generateTextDiff(anyString(), anyString()))
                .thenReturn(textDiffs);
        when(diffUtility.consolidateDiffs(anyList()))
                .thenReturn(textDiffs);

        // Act
        VersionComparisonResult result = versionService.compareVersions(version1.getId(), version2.getId());

        // Assert
        assertNotNull(result);
        assertEquals(version1.getId(), result.getVersionId1());
        assertEquals(version2.getId(), result.getVersionId2());
        assertEquals(version1.getContent(), result.getOriginalContent());
        assertEquals(version2.getContent(), result.getModifiedContent());
        assertEquals(textDiffs, result.getContentDiffs());

        // Should have one added parameter (new_param)
        assertEquals(1, result.getAddedParameters().size());
        assertEquals("new_param", result.getAddedParameters().get(0).getName());

        // Should have one modified parameter (parameter with updated description)
        assertEquals(1, result.getModifiedParameters().size());
        assertEquals("parameter", result.getModifiedParameters().get(0).getParameterName());
    }

    @Test
    @DisplayName("Should throw exception when comparing versions from different templates")
    void shouldThrowExceptionWhenComparingVersionsFromDifferentTemplates() {
        // Arrange
        PromptTemplate otherTemplate = PromptTemplate.builder()
                .id("other-template")
                .name("Other Template")
                .build();

        PromptVersion version1 = testVersion;
        PromptVersion version2 = PromptVersion.builder()
                .id("version-456")
                .template(otherTemplate)
                .versionNumber("1.0.0")
                .build();

        when(versionRepository.findByIdWithParameters(version1.getId()))
                .thenReturn(Optional.of(version1));
        when(versionRepository.findByIdWithParameters(version2.getId()))
                .thenReturn(Optional.of(version2));

        // Act & Assert
        assertThrows(ValidationException.class,
                () -> versionService.compareVersions(version1.getId(), version2.getId()));
    }

    @Test
    @DisplayName("Should rollback to previous version with history preservation")
    void shouldRollbackToVersionWithHistoryPreservation() {
        // Arrange
        when(versionRepository.findByIdWithParameters(PARENT_VERSION_ID))
                .thenReturn(Optional.of(parentVersion));
        when(versionRepository.save(any(PromptVersion.class)))
                .thenAnswer(i -> i.getArgument(0));

        // Act
        PromptVersion result = versionService.rollbackToVersion(PARENT_VERSION_ID, true);

        // Assert
        assertNotNull(result);
        assertNotEquals(PARENT_VERSION_ID, result.getId());  // Should be a new version
        assertEquals(parentVersion.getContent(), result.getContent());
        assertEquals(VersionStatus.DRAFT, result.getStatus());
        assertEquals(parentVersion.getId(), result.getParentVersion().getId());

        // Verify audit entry created
        ArgumentCaptor<VersionAuditEntry> auditCaptor = ArgumentCaptor.forClass(VersionAuditEntry.class);
        verify(auditRepository).save(auditCaptor.capture());
        VersionAuditEntry audit = auditCaptor.getValue();

        assertEquals(VersionAuditEntry.AuditActionType.ROLLBACK, audit.getActionType());
        assertEquals(PARENT_VERSION_ID, audit.getReferenceVersionId());
    }

    @Test
    @DisplayName("Should rollback to previous version without history preservation")
    void shouldRollbackToVersionWithoutHistoryPreservation() {
        // Arrange
        // Current active version
        PromptVersion currentVersion = PromptVersion.builder()
                .id("current-version")
                .template(testTemplate)
                .versionNumber("1.2.0")
                .content("Current content that will be replaced")
                .status(VersionStatus.DRAFT)
                .build();

        List<PromptVersion> activeVersions = Collections.singletonList(currentVersion);

        when(versionRepository.findByIdWithParameters(PARENT_VERSION_ID))
                .thenReturn(Optional.of(parentVersion));
        when(versionRepository.findByTemplateId(eq(TEMPLATE_ID), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(activeVersions));
        when(versionRepository.save(any(PromptVersion.class)))
                .thenAnswer(i -> i.getArgument(0));

        // Act
        PromptVersion result = versionService.rollbackToVersion(PARENT_VERSION_ID, false);

        // Assert
        assertNotNull(result);
        // Expecting the current version to be updated rather than creating a new one
        assertEquals(currentVersion.getId(), result.getId());
        assertEquals(parentVersion.getContent(), result.getContent());
        assertEquals(VersionStatus.DRAFT, result.getStatus());

        // Verify audit entry created
        ArgumentCaptor<VersionAuditEntry> auditCaptor = ArgumentCaptor.forClass(VersionAuditEntry.class);
        verify(auditRepository).save(auditCaptor.capture());
        VersionAuditEntry audit = auditCaptor.getValue();

        assertEquals(VersionAuditEntry.AuditActionType.ROLLBACK, audit.getActionType());
        assertEquals(PARENT_VERSION_ID, audit.getReferenceVersionId());
    }

    @Test
    @DisplayName("Should validate semantic version number correctly")
    void shouldValidateSemanticVersionNumberCorrectly() {
        // Test valid version numbers
        assertTrue(versionService.isValidVersionNumber("1.0.0"));
        assertTrue(versionService.isValidVersionNumber("0.1.0"));
        assertTrue(versionService.isValidVersionNumber("10.20.30"));

        // Test invalid version numbers
        assertFalse(versionService.isValidVersionNumber("1.0"));
        assertFalse(versionService.isValidVersionNumber("v1.0.0"));
        assertFalse(versionService.isValidVersionNumber("1.0.0-alpha"));
        assertFalse(versionService.isValidVersionNumber(""));
        assertFalse(versionService.isValidVersionNumber(null));
    }

    @Test
    @DisplayName("Should parse version number correctly")
    void shouldParseVersionNumberCorrectly() {
        // Act
        int[] components = versionService.parseVersionNumber("2.3.4");

        // Assert
        assertEquals(3, components.length);
        assertEquals(2, components[0]);  // Major
        assertEquals(3, components[1]);  // Minor
        assertEquals(4, components[2]);  // Patch
    }

    @Test
    @DisplayName("Should throw exception when parsing invalid version number")
    void shouldThrowExceptionWhenParsingInvalidVersion() {
        // Act & Assert
        assertThrows(ValidationException.class,
                () -> versionService.parseVersionNumber("invalid"));
    }
}
