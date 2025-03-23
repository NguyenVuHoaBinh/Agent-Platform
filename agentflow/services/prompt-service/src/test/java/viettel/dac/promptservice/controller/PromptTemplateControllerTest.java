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
import viettel.dac.promptservice.dto.request.PromptTemplateRequest;
import viettel.dac.promptservice.dto.response.PageResponse;
import viettel.dac.promptservice.dto.response.PromptTemplateResponse;
import viettel.dac.promptservice.dto.search.PromptTemplateSearchCriteria;
import viettel.dac.promptservice.exception.ResourceAlreadyExistsException;
import viettel.dac.promptservice.exception.ResourceNotFoundException;
import viettel.dac.promptservice.model.entity.PromptTemplate;
import viettel.dac.promptservice.service.PromptManagementService;
import viettel.dac.promptservice.service.mapper.EntityDtoMapper;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PromptTemplateControllerTest {

    @Mock
    private PromptManagementService promptManagementService;

    @Mock
    private EntityDtoMapper mapper;

    @InjectMocks
    private PromptTemplateController promptTemplateController;

    private PromptTemplate mockTemplate;
    private PromptTemplateResponse mockTemplateResponse;
    private PromptTemplateRequest mockTemplateRequest;
    private List<String> mockCategories;

    @BeforeEach
    void setUp() {
        // Initialize mock data
        mockTemplate = new PromptTemplate();
        mockTemplate.setId("template-id-1");
        mockTemplate.setName("Test Template");
        mockTemplate.setDescription("Test Description");
        mockTemplate.setProjectId("project-id-1");
        mockTemplate.setCategory("test-category");

        mockTemplateResponse = new PromptTemplateResponse();
        mockTemplateResponse.setId("template-id-1");
        mockTemplateResponse.setName("Test Template");
        mockTemplateResponse.setDescription("Test Description");
        mockTemplateResponse.setProjectId("project-id-1");
        mockTemplateResponse.setCategory("test-category");

        mockTemplateRequest = new PromptTemplateRequest();
        mockTemplateRequest.setName("Test Template");
        mockTemplateRequest.setDescription("Test Description");
        mockTemplateRequest.setProjectId("project-id-1");
        mockTemplateRequest.setCategory("test-category");

        mockCategories = Arrays.asList("category1", "category2", "category3");
    }

    @Test
    void createTemplate_Success() {
        // Arrange
        when(promptManagementService.createTemplate(any(PromptTemplateRequest.class))).thenReturn(mockTemplate);
        when(mapper.toTemplateResponse(any(PromptTemplate.class))).thenReturn(mockTemplateResponse);

        // Act
        ResponseEntity<PromptTemplateResponse> response = promptTemplateController.createTemplate(mockTemplateRequest);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(mockTemplateResponse, response.getBody());
        verify(promptManagementService).createTemplate(mockTemplateRequest);
        verify(mapper).toTemplateResponse(mockTemplate);
    }

    @Test
    void createTemplate_ValidationFailure() {
        // Arrange
        when(promptManagementService.createTemplate(any(PromptTemplateRequest.class)))
                .thenThrow(new ResourceAlreadyExistsException("Template already exists"));

        // Act & Assert
        assertThrows(ResourceAlreadyExistsException.class,
                () -> promptTemplateController.createTemplate(mockTemplateRequest));
        verify(promptManagementService).createTemplate(mockTemplateRequest);
    }

    @Test
    void getTemplateById_Found() {
        // Arrange
        when(promptManagementService.getTemplateById(anyString())).thenReturn(Optional.of(mockTemplate));
        when(mapper.toTemplateResponse(any(PromptTemplate.class))).thenReturn(mockTemplateResponse);

        // Act
        ResponseEntity<PromptTemplateResponse> response = promptTemplateController.getTemplateById("template-id-1", false);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockTemplateResponse, response.getBody());
        verify(promptManagementService).getTemplateById("template-id-1");
        verify(mapper).toTemplateResponse(mockTemplate);
    }

    @Test
    void getTemplateById_NotFound() {
        // Arrange
        when(promptManagementService.getTemplateById(anyString())).thenReturn(Optional.empty());

        // Act
        ResponseEntity<PromptTemplateResponse> response = promptTemplateController.getTemplateById("non-existent-id", false);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        verify(promptManagementService).getTemplateById("non-existent-id");
    }

    @Test
    void getTemplateById_WithVersions() {
        // Arrange
        when(promptManagementService.getTemplateWithVersions(anyString())).thenReturn(Optional.of(mockTemplate));
        when(mapper.toTemplateResponseWithVersions(any(PromptTemplate.class))).thenReturn(mockTemplateResponse);

        // Act
        ResponseEntity<PromptTemplateResponse> response = promptTemplateController.getTemplateById("template-id-1", true);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockTemplateResponse, response.getBody());
        verify(promptManagementService).getTemplateWithVersions("template-id-1");
        verify(mapper).toTemplateResponseWithVersions(mockTemplate);
    }

    @Test
    void updateTemplate_Success() {
        // Arrange
        when(promptManagementService.updateTemplate(anyString(), any(PromptTemplateRequest.class))).thenReturn(mockTemplate);
        when(mapper.toTemplateResponse(any(PromptTemplate.class))).thenReturn(mockTemplateResponse);

        // Act
        ResponseEntity<PromptTemplateResponse> response = promptTemplateController.updateTemplate("template-id-1", mockTemplateRequest);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockTemplateResponse, response.getBody());
        verify(promptManagementService).updateTemplate("template-id-1", mockTemplateRequest);
        verify(mapper).toTemplateResponse(mockTemplate);
    }

    @Test
    void updateTemplate_NotFound() {
        // Arrange
        when(promptManagementService.updateTemplate(anyString(), any(PromptTemplateRequest.class)))
                .thenThrow(new ResourceNotFoundException("Template not found"));

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> promptTemplateController.updateTemplate("non-existent-id", mockTemplateRequest));
        verify(promptManagementService).updateTemplate("non-existent-id", mockTemplateRequest);
    }

    @Test
    void deleteTemplate_Success() {
        // Arrange
        doNothing().when(promptManagementService).deleteTemplate(anyString());

        // Act
        ResponseEntity<Void> response = promptTemplateController.deleteTemplate("template-id-1");

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(promptManagementService).deleteTemplate("template-id-1");
    }

    @Test
    void deleteTemplate_NotFound() {
        // Arrange
        doThrow(new ResourceNotFoundException("Template not found"))
                .when(promptManagementService).deleteTemplate(anyString());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> promptTemplateController.deleteTemplate("non-existent-id"));
        verify(promptManagementService).deleteTemplate("non-existent-id");
    }

    @Test
    void getTemplatesByProject() {
        // Arrange
        List<PromptTemplate> templates = Arrays.asList(mockTemplate);
        Page<PromptTemplate> templatePage = new PageImpl<>(templates);
        PageResponse<PromptTemplateResponse> pageResponse = new PageResponse<>();

        when(promptManagementService.getTemplatesByProject(anyString(), any(Pageable.class))).thenReturn(templatePage);
        when(mapper.toPageResponse(any(Page.class), any(Function.class))).thenReturn(pageResponse);

        // Act
        ResponseEntity<PageResponse<PromptTemplateResponse>> response =
                promptTemplateController.getTemplatesByProject("project-id-1", Pageable.unpaged());

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(pageResponse, response.getBody());
        verify(promptManagementService).getTemplatesByProject("project-id-1", Pageable.unpaged());
    }

    @Test
    void getTemplatesByCategory() {
        // Arrange
        List<PromptTemplate> templates = Arrays.asList(mockTemplate);
        Page<PromptTemplate> templatePage = new PageImpl<>(templates);
        PageResponse<PromptTemplateResponse> pageResponse = new PageResponse<>();

        when(promptManagementService.getTemplatesByCategory(anyString(), any(Pageable.class))).thenReturn(templatePage);
        when(mapper.toPageResponse(any(Page.class), any(Function.class))).thenReturn(pageResponse);

        // Act
        ResponseEntity<PageResponse<PromptTemplateResponse>> response =
                promptTemplateController.getTemplatesByCategory("test-category", Pageable.unpaged());

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(pageResponse, response.getBody());
        verify(promptManagementService).getTemplatesByCategory("test-category", Pageable.unpaged());
    }

    @Test
    void searchTemplates() {
        // Arrange
        List<PromptTemplate> templates = Arrays.asList(mockTemplate);
        Page<PromptTemplate> templatePage = new PageImpl<>(templates);
        PageResponse<PromptTemplateResponse> pageResponse = new PageResponse<>();

        when(promptManagementService.searchTemplates(any(PromptTemplateSearchCriteria.class), any(Pageable.class)))
                .thenReturn(templatePage);
        when(mapper.toPageResponse(any(Page.class), any(Function.class))).thenReturn(pageResponse);

        // Act
        ResponseEntity<PageResponse<PromptTemplateResponse>> response =
                promptTemplateController.searchTemplates("search", "project-id-1", "category", "user", true, 5, true, false, Pageable.unpaged());

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(pageResponse, response.getBody());
        verify(promptManagementService).searchTemplates(any(PromptTemplateSearchCriteria.class), eq(Pageable.unpaged()));
    }

    @Test
    void getAllCategories() {
        // Arrange
        when(promptManagementService.getAllCategories()).thenReturn(mockCategories);

        // Act
        ResponseEntity<List<String>> response = promptTemplateController.getAllCategories();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockCategories, response.getBody());
        verify(promptManagementService).getAllCategories();
    }
}