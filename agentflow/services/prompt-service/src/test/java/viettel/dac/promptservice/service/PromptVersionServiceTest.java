package viettel.dac.promptservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import viettel.dac.promptservice.dto.request.PromptParameterRequest;
import viettel.dac.promptservice.dto.request.PromptVersionRequest;
import viettel.dac.promptservice.exception.ValidationException;
import viettel.dac.promptservice.model.entity.PromptTemplate;
import viettel.dac.promptservice.model.entity.PromptVersion;
import viettel.dac.promptservice.model.enums.ParameterType;
import viettel.dac.promptservice.model.enums.VersionStatus;
import viettel.dac.promptservice.repository.jpa.PromptParameterRepository;
import viettel.dac.promptservice.repository.jpa.PromptTemplateRepository;
import viettel.dac.promptservice.repository.jpa.PromptVersionRepository;
import viettel.dac.promptservice.security.SecurityUtils;
import viettel.dac.promptservice.service.event.VersionStatusChangeEvent;
import viettel.dac.promptservice.service.impl.PromptVersionServiceImpl;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PromptVersionServiceTest {

    @Mock
    private PromptVersionRepository versionRepository;

    @Mock
    private PromptTemplateRepository templateRepository;

    @Mock
    private PromptParameterRepository parameterRepository;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PromptVersionServiceImpl service;

    private PromptVersionRequest validRequest;
    private PromptTemplate template;
    private PromptVersion existingVersion;
    private PromptVersion publishedVersion;
    private String userId = "test-user-id";

    @BeforeEach
    void setUp() {
        // Set up a template
        template = new PromptTemplate();
        template.setId("test-template-id");
        template.setName("Test Template");
        template.setProjectId("test-project-id");

        // Set up an existing version
        existingVersion = new PromptVersion();
        existingVersion.setId("test-version-id");
        existingVersion.setTemplate(template);
        existingVersion.setVersionNumber("1.0.0");
        existingVersion.setContent("Test content");
        existingVersion.setStatus(VersionStatus.DRAFT);
        existingVersion.setCreatedBy(userId);
        existingVersion.setCreatedAt(LocalDateTime.now().minusDays(1));

        // Set up a published version
        publishedVersion = new PromptVersion();
        publishedVersion.setId("published-version-id");
        publishedVersion.setTemplate(template);
        publishedVersion.setVersionNumber("0.9.0");
        publishedVersion.setContent("Published content");
        publishedVersion.setStatus(VersionStatus.PUBLISHED);
        publishedVersion.setCreatedBy(userId);
        publishedVersion.setCreatedAt(LocalDateTime.now().minusDays(7));

        // Set up a valid version request
        validRequest = new PromptVersionRequest();
        validRequest.setTemplateId("test-template-id");
        validRequest.setVersionNumber("1.1.0");
        validRequest.setContent("New version content");

        // Add a parameter to the request
        PromptParameterRequest paramRequest = new PromptParameterRequest();
        paramRequest.setName("testParam");
        paramRequest.setDescription("Test parameter");
        paramRequest.setParameterType(ParameterType.STRING);
        paramRequest.setRequired(true);
        validRequest.setParameters(List.of(paramRequest));

        // Configure security utils to return test user
        when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(userId));
    }

    @Test
    void testCreateVersion_Success() {
        // Given
        when(templateRepository.findById("test-template-id")).thenReturn(Optional.of(template));
        when(versionRepository.findByTemplateIdAndVersionNumber("test-template-id", "1.1.0"))
                .thenReturn(Optional.empty());
        when(versionRepository.save(any(PromptVersion.class))).thenAnswer(invocation -> {
            PromptVersion version = invocation.getArgument(0);
            if (version.getId() == null) {
                version.setId("new-version-id");
            }
            version.setCreatedAt(LocalDateTime.now());
            return version;
        });

        // When
        PromptVersion result = service.createVersion(validRequest);

        // Then
        assertNotNull(result);
        assertEquals("new-version-id", result.getId());
        assertEquals("1.1.0", result.getVersionNumber());
        assertEquals(validRequest.getContent(), result.getContent());
        assertEquals(VersionStatus.DRAFT, result.getStatus());
        assertEquals(userId, result.getCreatedBy());
        assertNull(result.getParentVersion());
        assertNotNull(result.getParameters());

        // Verify interactions
        verify(templateRepository).findById("test-template-id");
        verify(versionRepository).findByTemplateIdAndVersionNumber("test-template-id", "1.1.0");
        verify(versionRepository, times(2)).save(any(PromptVersion.class));
    }

    @Test
    void testCreateVersion_WithParent() {
        // Given
        validRequest.setParentVersionId("test-version-id");

        when(templateRepository.findById("test-template-id")).thenReturn(Optional.of(template));
        when(versionRepository.findById("test-version-id")).thenReturn(Optional.of(existingVersion));
        when(versionRepository.findByTemplateIdAndVersionNumber("test-template-id", "1.1.0"))
                .thenReturn(Optional.empty());
        when(versionRepository.save(any(PromptVersion.class))).thenAnswer(invocation -> {
            PromptVersion version = invocation.getArgument(0);
            if (version.getId() == null) {
                version.setId("new-version-id");
            }
            version.setCreatedAt(LocalDateTime.now());
            return version;
        });

        // When
        PromptVersion result = service.createVersion(validRequest);

        // Then
        assertNotNull(result);
        assertEquals("new-version-id", result.getId());
        assertEquals("1.1.0", result.getVersionNumber());
        assertEquals(existingVersion, result.getParentVersion());

        // Verify interactions
        verify(templateRepository).findById("test-template-id");
        verify(versionRepository).findById("test-version-id");
        verify(versionRepository).findByTemplateIdAndVersionNumber("test-template-id", "1.1.0");
        verify(versionRepository, times(2)).save(any(PromptVersion.class));
    }

    @Test
    void testCreateVersion_InvalidVersionNumber() {
        // Given
        validRequest.setVersionNumber("1.0"); // Invalid - missing patch version

        // When & Then
        assertThrows(ValidationException.class, () -> {
            service.createVersion(validRequest);
        });

        // Verify interactions
        verify(templateRepository, never()).findById(anyString());
        verify(versionRepository, never()).save(any(PromptVersion.class));
    }

    @Test
    void testCreateVersion_DuplicateVersionNumber() {
        // Given
        when(templateRepository.findById("test-template-id")).thenReturn(Optional.of(template));
        when(versionRepository.findByTemplateIdAndVersionNumber("test-template-id", "1.1.0"))
                .thenReturn(Optional.of(existingVersion)); // Simulating existing version

        // When & Then
        assertThrows(ValidationException.class, () -> {
            service.createVersion(validRequest);
        });

        // Verify interactions
        verify(templateRepository).findById("test-template-id");
        verify(versionRepository).findByTemplateIdAndVersionNumber("test-template-id", "1.1.0");
        verify(versionRepository, never()).save(any(PromptVersion.class));
    }

    @Test
    void testCreateBranch_Success() {
        // Given
        PromptVersionRequest branchRequest = new PromptVersionRequest();
        branchRequest.setVersionNumber("2.0.0");

        when(versionRepository.findByIdWithParameters("test-version-id"))
                .thenReturn(Optional.of(existingVersion));
        when(versionRepository.findByTemplateIdAndVersionNumber("test-template-id", "2.0.0"))
                .thenReturn(Optional.empty());
        when(versionRepository.save(any(PromptVersion.class))).thenAnswer(invocation -> {
            PromptVersion version = invocation.getArgument(0);
            if (version.getId() == null) {
                version.setId("branch-version-id");
            }
            version.setCreatedAt(LocalDateTime.now());
            return version;
        });

        // When
        PromptVersion result = service.createBranch("test-version-id", branchRequest);

        // Then
        assertNotNull(result);
        assertEquals("branch-version-id", result.getId());
        assertEquals("2.0.0", result.getVersionNumber());
        assertEquals(existingVersion.getContent(), result.getContent()); // Inherits content
        assertEquals(VersionStatus.DRAFT, result.getStatus());
        assertEquals(existingVersion, result.getParentVersion());

        // Verify interactions
        verify(versionRepository).findByIdWithParameters("test-version-id");
        verify(versionRepository).findByTemplateIdAndVersionNumber("test-template-id", "2.0.0");
        verify(versionRepository, times(2)).save(any(PromptVersion.class));
    }

    @Test
    void testUpdateVersionStatus_Success() {
        // Given
        when(versionRepository.findById("test-version-id")).thenReturn(Optional.of(existingVersion));
        when(versionRepository.save(any(PromptVersion.class))).thenReturn(existingVersion);

        // When
        PromptVersion result = service.updateVersionStatus("test-version-id", VersionStatus.REVIEW);

        // Then
        assertNotNull(result);
        assertEquals(VersionStatus.REVIEW, result.getStatus());

        // Verify interactions
        verify(versionRepository).findById("test-version-id");
        verify(versionRepository).save(existingVersion);
        verify(eventPublisher).publishEvent(any(VersionStatusChangeEvent.class));
    }

    @Test
    void testUpdateVersionStatus_PublishedVersion() {
        // Given
        when(versionRepository.findById("test-version-id")).thenReturn(Optional.of(existingVersion));
        when(versionRepository.findByTemplateIdAndStatus(
                eq("test-template-id"),
                eq(VersionStatus.PUBLISHED),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(publishedVersion)));
        when(versionRepository.save(any(PromptVersion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        PromptVersion result = service.updateVersionStatus("test-version-id", VersionStatus.PUBLISHED);

        // Then
        assertNotNull(result);
        assertEquals(VersionStatus.PUBLISHED, result.getStatus());

        // Verify interactions
        verify(versionRepository).findById("test-version-id");
        verify(versionRepository).findByTemplateIdAndStatus(
                eq("test-template-id"),
                eq(VersionStatus.PUBLISHED),
                any(Pageable.class));
        verify(versionRepository, times(2)).save(any(PromptVersion.class)); // One for existingVersion, one for publishedVersion
        verify(eventPublisher, times(2)).publishEvent(any(VersionStatusChangeEvent.class)); // Two events
    }

    @Test
    void testUpdateVersionStatus_InvalidTransition() {
        // Given
        existingVersion.setStatus(VersionStatus.ARCHIVED); // Archived versions can't transition to any other status
        when(versionRepository.findById("test-version-id")).thenReturn(Optional.of(existingVersion));

        // When & Then
        assertThrows(ValidationException.class, () -> {
            service.updateVersionStatus("test-version-id", VersionStatus.PUBLISHED);
        });

        // Verify interactions
        verify(versionRepository).findById("test-version-id");
        verify(versionRepository, never()).save(any(PromptVersion.class));
        verify(eventPublisher, never()).publishEvent(any(VersionStatusChangeEvent.class));
    }

    @Test
    void testGetVersionLineage() {
        // Given
        List<PromptVersion> lineage = Arrays.asList(existingVersion);

        when(versionRepository.existsById("test-version-id")).thenReturn(true);
        when(versionRepository.findVersionLineage("test-version-id")).thenReturn(lineage);

        // When
        List<PromptVersion> result = service.getVersionLineage("test-version-id");

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(existingVersion, result.get(0));

        // Verify interactions
        verify(versionRepository).existsById("test-version-id");
        verify(versionRepository).findVersionLineage("test-version-id");
    }

    @Test
    void testParseVersionNumber() {
        // When
        int[] result = service.parseVersionNumber("2.3.4");

        // Then
        assertArrayEquals(new int[] {2, 3, 4}, result);
    }

    @Test
    void testIsValidVersionNumber() {
        // Valid version numbers
        assertTrue(service.isValidVersionNumber("0.0.0"));
        assertTrue(service.isValidVersionNumber("1.0.0"));
        assertTrue(service.isValidVersionNumber("10.20.30"));

        // Invalid version numbers
        assertFalse(service.isValidVersionNumber("1.0"));
        assertFalse(service.isValidVersionNumber("1.0.0.0"));
        assertFalse(service.isValidVersionNumber("1.0.x"));
        assertFalse(service.isValidVersionNumber("01.1.0")); // Leading zeros not allowed
        assertFalse(service.isValidVersionNumber("v1.0.0"));
        assertFalse(service.isValidVersionNumber(null));
    }
}