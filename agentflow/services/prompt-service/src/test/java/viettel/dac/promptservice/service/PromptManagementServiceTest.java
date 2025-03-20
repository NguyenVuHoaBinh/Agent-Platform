package viettel.dac.promptservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import viettel.dac.promptservice.dto.request.PromptTemplateRequest;
import viettel.dac.promptservice.dto.search.PromptTemplateSearchCriteria;
import viettel.dac.promptservice.exception.ResourceAlreadyExistsException;
import viettel.dac.promptservice.exception.ResourceNotFoundException;
import viettel.dac.promptservice.exception.ValidationException;
import viettel.dac.promptservice.model.entity.PromptTemplate;
import viettel.dac.promptservice.repository.jpa.PromptTemplateRepository;
import viettel.dac.promptservice.repository.jpa.PromptVersionRepository;
import viettel.dac.promptservice.security.SecurityUtils;
import viettel.dac.promptservice.service.elasticsearch.PromptSearchService;
import viettel.dac.promptservice.service.impl.PromptManagementServiceImpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PromptManagementServiceTest {

    @Mock
    private PromptTemplateRepository templateRepository;

    @Mock
    private PromptVersionRepository versionRepository;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private PromptSearchService searchService;

    @InjectMocks
    private PromptManagementServiceImpl service;

    private PromptTemplateRequest validRequest;
    private PromptTemplate existingTemplate;
    private String userId = "test-user-id";

    @BeforeEach
    void setUp() {
        validRequest = new PromptTemplateRequest();
        validRequest.setName("Test Template");
        validRequest.setDescription("Test Description");
        validRequest.setProjectId("test-project-id");
        validRequest.setCategory("Test Category");

        existingTemplate = new PromptTemplate();
        existingTemplate.setId("test-template-id");
        existingTemplate.setName("Existing Template");
        existingTemplate.setDescription("Existing Description");
        existingTemplate.setProjectId("test-project-id");
        existingTemplate.setCategory("Test Category");
        existingTemplate.setCreatedBy(userId);
        existingTemplate.setCreatedAt(LocalDateTime.now().minusDays(1));
        existingTemplate.setUpdatedAt(LocalDateTime.now().minusDays(1));

        // Use lenient to prevent UnnecessaryStubbingException
        lenient().when(securityUtils.getCurrentUserId()).thenReturn(Optional.of(userId));
    }


    @Test
    void testCreateTemplate_Success() {
        // Given
        when(templateRepository.existsByNameAndProjectId(anyString(), anyString())).thenReturn(false);
        when(templateRepository.save(any(PromptTemplate.class))).thenAnswer(invocation -> {
            PromptTemplate template = invocation.getArgument(0);
            template.setId("new-template-id");
            template.setCreatedAt(LocalDateTime.now());
            template.setUpdatedAt(LocalDateTime.now());
            return template;
        });

        // When
        PromptTemplate result = service.createTemplate(validRequest);

        // Then
        assertNotNull(result);
        assertEquals("new-template-id", result.getId());
        assertEquals(validRequest.getName(), result.getName());
        assertEquals(validRequest.getDescription(), result.getDescription());
        assertEquals(validRequest.getProjectId(), result.getProjectId());
        assertEquals(validRequest.getCategory(), result.getCategory());
        assertEquals(userId, result.getCreatedBy());

        // Verify interactions
        verify(templateRepository).existsByNameAndProjectId(validRequest.getName(), validRequest.getProjectId());
        verify(templateRepository).save(any(PromptTemplate.class));
        verify(searchService).indexPromptTemplate(any(PromptTemplate.class));
    }

    @Test
    void testCreateTemplate_DuplicateName() {
        // Given
        when(templateRepository.existsByNameAndProjectId(anyString(), anyString())).thenReturn(true);

        // When & Then
        assertThrows(ResourceAlreadyExistsException.class, () -> {
            service.createTemplate(validRequest);
        });

        // Verify interactions
        verify(templateRepository).existsByNameAndProjectId(validRequest.getName(), validRequest.getProjectId());
        verify(templateRepository, never()).save(any(PromptTemplate.class));
        verify(searchService, never()).indexPromptTemplate(any(PromptTemplate.class));
    }

    @Test
    void testCreateTemplate_InvalidName() {
        // Given
        validRequest.setName("Invalid@Name");

        // When & Then
        assertThrows(ValidationException.class, () -> {
            service.createTemplate(validRequest);
        });

        // Verify interactions
        verify(templateRepository, never()).existsByNameAndProjectId(anyString(), anyString());
        verify(templateRepository, never()).save(any(PromptTemplate.class));
        verify(searchService, never()).indexPromptTemplate(any(PromptTemplate.class));
    }

    @Test
    void testUpdateTemplate_Success() {
        // Given
        when(templateRepository.findById("test-template-id")).thenReturn(Optional.of(existingTemplate));
        when(templateRepository.existsByNameAndProjectId(anyString(), anyString())).thenReturn(false);
        when(templateRepository.save(any(PromptTemplate.class))).thenAnswer(invocation -> {
            PromptTemplate template = invocation.getArgument(0);
            template.setUpdatedAt(LocalDateTime.now());
            return template;
        });

        PromptTemplateRequest updateRequest = new PromptTemplateRequest();
        updateRequest.setName("Updated Name");
        updateRequest.setDescription("Updated Description");
        updateRequest.setProjectId("test-project-id");
        updateRequest.setCategory("Updated Category");

        // When
        PromptTemplate result = service.updateTemplate("test-template-id", updateRequest);

        // Then
        assertNotNull(result);
        assertEquals("test-template-id", result.getId());
        assertEquals(updateRequest.getName(), result.getName());
        assertEquals(updateRequest.getDescription(), result.getDescription());
        assertEquals(updateRequest.getCategory(), result.getCategory());

        // Verify interactions
        verify(templateRepository).findById("test-template-id");
        verify(templateRepository).existsByNameAndProjectId(updateRequest.getName(), existingTemplate.getProjectId());
        verify(templateRepository).save(any(PromptTemplate.class));
        verify(searchService).indexPromptTemplate(any(PromptTemplate.class));
    }

    @Test
    void testUpdateTemplate_NotFound() {
        // Given
        when(templateRepository.findById("non-existent-id")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            service.updateTemplate("non-existent-id", validRequest);
        });

        // Verify interactions
        verify(templateRepository).findById("non-existent-id");
        verify(templateRepository, never()).save(any(PromptTemplate.class));
        verify(searchService, never()).indexPromptTemplate(any(PromptTemplate.class));
    }

    @Test
    void testUpdateTemplate_DuplicateName() {
        // Given
        when(templateRepository.findById("test-template-id")).thenReturn(Optional.of(existingTemplate));
        when(templateRepository.existsByNameAndProjectId("New Name", existingTemplate.getProjectId())).thenReturn(true);

        PromptTemplateRequest updateRequest = new PromptTemplateRequest();
        updateRequest.setName("New Name");
        updateRequest.setDescription("Updated Description");
        updateRequest.setProjectId("test-project-id");

        // When & Then
        assertThrows(ResourceAlreadyExistsException.class, () -> {
            service.updateTemplate("test-template-id", updateRequest);
        });

        // Verify interactions
        verify(templateRepository).findById("test-template-id");
        verify(templateRepository).existsByNameAndProjectId("New Name", existingTemplate.getProjectId());
        verify(templateRepository, never()).save(any(PromptTemplate.class));
        verify(searchService, never()).indexPromptTemplate(any(PromptTemplate.class));
    }

    @Test
    void testDeleteTemplate_Success() {
        // Given
        when(templateRepository.findByIdWithVersions("test-template-id")).thenReturn(Optional.of(existingTemplate));

        // When
        service.deleteTemplate("test-template-id");

        // Then
        verify(templateRepository).findByIdWithVersions("test-template-id");
        verify(templateRepository).delete(existingTemplate);
        verify(searchService).deleteTemplateIndex("test-template-id");
    }

    @Test
    void testDeleteTemplate_NotFound() {
        // Given
        when(templateRepository.findByIdWithVersions("non-existent-id")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ResourceNotFoundException.class, () -> {
            service.deleteTemplate("non-existent-id");
        });

        // Verify interactions
        verify(templateRepository).findByIdWithVersions("non-existent-id");
        verify(templateRepository, never()).delete(any(PromptTemplate.class));
        verify(searchService, never()).deleteTemplateIndex(anyString());
    }

    @Test
    void testSearchTemplates_DatabaseSearch() {
        Pageable pageable = PageRequest.of(0, 10);
        PromptTemplateSearchCriteria criteria = new PromptTemplateSearchCriteria();
        criteria.setSearchText("Test");
        criteria.setProjectId("test-project-id");

        List<PromptTemplate> templates = new ArrayList<>();
        templates.add(existingTemplate);
        Page<PromptTemplate> templatesPage = new PageImpl<>(templates, pageable, 1);

        // Fix argument mismatch by using argument matchers
        when(templateRepository.search(anyString(), anyString(), any(), any(Pageable.class)))
                .thenReturn(templatesPage);

        // Simulate Elasticsearch failure
        doThrow(new RuntimeException("Elasticsearch unavailable"))
                .when(searchService).searchTemplates(any(), any());

        // When
        Page<PromptTemplate> result = service.searchTemplates(criteria, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(existingTemplate, result.getContent().get(0));

        // Verify interactions
        verify(searchService).searchTemplates(criteria, pageable);
        verify(templateRepository).search(anyString(), anyString(), any(), any(Pageable.class));
    }



    @Test
    void testSearchTemplates_ElasticsearchSearch() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        PromptTemplateSearchCriteria criteria = new PromptTemplateSearchCriteria();
        criteria.setSearchText("Test");
        criteria.setProjectId("test-project-id");
        criteria.setFromDate(LocalDateTime.now().minusDays(7));

        List<PromptTemplate> templates = new ArrayList<>();
        templates.add(existingTemplate);
        Page<PromptTemplate> templatesPage = new PageImpl<>(templates, pageable, 1);

        when(searchService.searchTemplates(any(), any())).thenReturn(templatesPage);

        // When
        Page<PromptTemplate> result = service.searchTemplates(criteria, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(existingTemplate, result.getContent().get(0));

        // Verify interactions
        verify(searchService).searchTemplates(criteria, pageable);
        verify(templateRepository, never()).search(anyString(), anyString(), anyString(), any(Pageable.class));
    }
}