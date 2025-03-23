package viettel.dac.promptservice.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import viettel.dac.promptservice.dto.request.PromptVersionRequest;
import viettel.dac.promptservice.dto.response.PageResponse;
import viettel.dac.promptservice.dto.response.PromptVersionResponse;
import viettel.dac.promptservice.dto.response.VersionComparisonResult;
import viettel.dac.promptservice.exception.ResourceNotFoundException;
import viettel.dac.promptservice.exception.ValidationException;
import viettel.dac.promptservice.model.entity.PromptTemplate;
import viettel.dac.promptservice.model.entity.PromptVersion;
import viettel.dac.promptservice.model.entity.VersionAuditEntry;
import viettel.dac.promptservice.model.enums.VersionStatus;
import viettel.dac.promptservice.service.PromptVersionService;
import viettel.dac.promptservice.service.mapper.EntityDtoMapper;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PromptVersionControllerTest {

    @Mock
    private PromptVersionService versionService;

    @Mock
    private EntityDtoMapper mapper;

    @InjectMocks
    private PromptVersionController versionController;

    private PromptTemplate mockTemplate;
    private PromptVersion mockVersion;
    private PromptVersionResponse mockVersionResponse;
    private PromptVersionRequest mockVersionRequest;
    private List<VersionAuditEntry> mockAuditEntries;

    @BeforeEach
    void setUp() {
        // Initialize mock data
        mockTemplate = new PromptTemplate();
        mockTemplate.setId("template-id-1");
        mockTemplate.setName("Test Template");

        mockVersion = new PromptVersion();
        mockVersion.setId("version-id-1");
        mockVersion.setTemplate(mockTemplate);
        mockVersion.setVersionNumber("1.0.0");
        mockVersion.setContent("Test content");
        mockVersion.setStatus(VersionStatus.DRAFT);

        mockVersionResponse = new PromptVersionResponse();
        mockVersionResponse.setId("version-id-1");
        mockVersionResponse.setTemplateId("template-id-1");
        mockVersionResponse.setVersionNumber("1.0.0");
        mockVersionResponse.setContent("Test content");
        mockVersionResponse.setStatus(VersionStatus.DRAFT);

        mockVersionRequest = new PromptVersionRequest();
        mockVersionRequest.setTemplateId("template-id-1");
        mockVersionRequest.setVersionNumber("1.0.0");
        mockVersionRequest.setContent("Test content");

        mockAuditEntries = Arrays.asList(
                new VersionAuditEntry()
        );
    }

    @Test
    void createVersion_Success() {
        // Arrange
        when(versionService.createVersion(any(PromptVersionRequest.class))).thenReturn(mockVersion);
        when(mapper.toVersionResponse(any(PromptVersion.class))).thenReturn(mockVersionResponse);

        // Act
        ResponseEntity<PromptVersionResponse> response = versionController.createVersion(mockVersionRequest);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(mockVersionResponse, response.getBody());
        verify(versionService).createVersion(mockVersionRequest);
        verify(mapper).toVersionResponse(mockVersion);
    }

    @Test
    void createVersion_ValidationFailure() {
        // Arrange
        when(versionService.createVersion(any(PromptVersionRequest.class)))
                .thenThrow(new ValidationException("Invalid version number"));

        // Act & Assert
        assertThrows(ValidationException.class,
                () -> versionController.createVersion(mockVersionRequest));
        verify(versionService).createVersion(mockVersionRequest);
    }

    @Test
    void getVersionById_Found() {
        // Arrange
        when(versionService.getVersionWithParameters(anyString())).thenReturn(Optional.of(mockVersion));
        when(mapper.toVersionResponse(any(PromptVersion.class))).thenReturn(mockVersionResponse);

        // Act
        ResponseEntity<PromptVersionResponse> response = versionController.getVersionById("version-id-1");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockVersionResponse, response.getBody());
        verify(versionService).getVersionWithParameters("version-id-1");
        verify(mapper).toVersionResponse(mockVersion);
    }

    @Test
    void getVersionById_NotFound() {
        // Arrange
        when(versionService.getVersionWithParameters(anyString())).thenReturn(Optional.empty());

        // Act
        ResponseEntity<PromptVersionResponse> response = versionController.getVersionById("non-existent-id");

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        verify(versionService).getVersionWithParameters("non-existent-id");
    }

    @Test
    void getVersionsByTemplate() {
        // Arrange
        List<PromptVersion> versions = Arrays.asList(mockVersion);
        Page<PromptVersion> versionPage = new PageImpl<>(versions);
        PageResponse<PromptVersionResponse> pageResponse = new PageResponse<>();

        when(versionService.getVersionsByTemplate(anyString(), any(Pageable.class))).thenReturn(versionPage);
        when(mapper.toPageResponse(any(Page.class), any(Function.class))).thenReturn(pageResponse);

        // Act
        ResponseEntity<PageResponse<PromptVersionResponse>> response =
                versionController.getVersionsByTemplate("template-id-1", Pageable.unpaged());

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(pageResponse, response.getBody());
        verify(versionService).getVersionsByTemplate("template-id-1", Pageable.unpaged());
    }

    @Test
    void updateVersionStatus_Success() {
        // Arrange
        when(versionService.canTransitionToStatus(anyString(), any(VersionStatus.class))).thenReturn(true);
        when(versionService.updateVersionStatus(anyString(), any(VersionStatus.class))).thenReturn(mockVersion);
        when(mapper.toVersionResponse(any(PromptVersion.class))).thenReturn(mockVersionResponse);

        // Act
        ResponseEntity<PromptVersionResponse> response =
                versionController.updateVersionStatus("version-id-1", VersionStatus.REVIEW);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockVersionResponse, response.getBody());
        verify(versionService).canTransitionToStatus("version-id-1", VersionStatus.REVIEW);
        verify(versionService).updateVersionStatus("version-id-1", VersionStatus.REVIEW);
        verify(mapper).toVersionResponse(mockVersion);
    }

    @Test
    void updateVersionStatus_InvalidTransition() {
        // Arrange
        when(versionService.canTransitionToStatus(anyString(), any(VersionStatus.class))).thenReturn(false);

        // Act
        ResponseEntity<PromptVersionResponse> response =
                versionController.updateVersionStatus("version-id-1", VersionStatus.PUBLISHED);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNull(response.getBody());
        verify(versionService).canTransitionToStatus("version-id-1", VersionStatus.PUBLISHED);
        verify(versionService, never()).updateVersionStatus(anyString(), any(VersionStatus.class));
    }

    @Test
    void createBranch() {
        // Arrange
        when(versionService.createBranch(anyString(), any(PromptVersionRequest.class))).thenReturn(mockVersion);
        when(mapper.toVersionResponse(any(PromptVersion.class))).thenReturn(mockVersionResponse);

        // Act
        ResponseEntity<PromptVersionResponse> response =
                versionController.createBranch("source-version-id", mockVersionRequest);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(mockVersionResponse, response.getBody());
        verify(versionService).createBranch("source-version-id", mockVersionRequest);
        verify(mapper).toVersionResponse(mockVersion);
    }

    @Test
    void compareVersions() {
        // Arrange
        VersionComparisonResult comparisonResult = VersionComparisonResult.builder()
                .versionId1("version-id-1")
                .versionNumber1("1.0.0")
                .versionId2("version-id-2")
                .versionNumber2("1.1.0")
                .build();

        when(versionService.compareVersions(anyString(), anyString())).thenReturn(comparisonResult);

        // Act
        ResponseEntity<VersionComparisonResult> response =
                versionController.compareVersions("version-id-1", "version-id-2");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(comparisonResult, response.getBody());
        verify(versionService).compareVersions("version-id-1", "version-id-2");
    }

    @Test
    void rollbackToVersion() {
        // Arrange
        when(versionService.rollbackToVersion(anyString(), anyBoolean())).thenReturn(mockVersion);
        when(mapper.toVersionResponse(any(PromptVersion.class))).thenReturn(mockVersionResponse);

        // Act
        ResponseEntity<PromptVersionResponse> response =
                versionController.rollbackToVersion("version-id-1", true);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockVersionResponse, response.getBody());
        verify(versionService).rollbackToVersion("version-id-1", true);
        verify(mapper).toVersionResponse(mockVersion);
    }

    @Test
    void getVersionLineage() {
        // Arrange
        List<PromptVersion> lineage = Arrays.asList(mockVersion);
        List<PromptVersionResponse> lineageResponse = Arrays.asList(mockVersionResponse);

        when(versionService.getVersionLineage(anyString())).thenReturn(lineage);
        when(mapper.toVersionResponse(any(PromptVersion.class))).thenReturn(mockVersionResponse);

        // Act
        ResponseEntity<List<PromptVersionResponse>> response = versionController.getVersionLineage("version-id-1");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals(mockVersionResponse, response.getBody().get(0));
        verify(versionService).getVersionLineage("version-id-1");
        verify(mapper).toVersionResponse(mockVersion);
    }

    @Test
    void getVersionAuditTrail() {
        // Arrange
        when(versionService.getVersionAuditTrail(anyString())).thenReturn(mockAuditEntries);

        // Act
        ResponseEntity<List<VersionAuditEntry>> response = versionController.getVersionAuditTrail("version-id-1");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockAuditEntries, response.getBody());
        verify(versionService).getVersionAuditTrail("version-id-1");
    }

    @Test
    void getVersionHistory() {
        // Arrange
        List<PromptVersion> history = Arrays.asList(mockVersion);
        List<PromptVersionResponse> historyResponse = Arrays.asList(mockVersionResponse);

        when(versionService.getVersionHistory(anyString())).thenReturn(history);
        when(mapper.toVersionResponse(any(PromptVersion.class))).thenReturn(mockVersionResponse);

        // Act
        ResponseEntity<List<PromptVersionResponse>> response = versionController.getVersionHistory("template-id-1");

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        assertEquals(mockVersionResponse, response.getBody().get(0));
        verify(versionService).getVersionHistory("template-id-1");
        verify(mapper).toVersionResponse(mockVersion);
    }
}