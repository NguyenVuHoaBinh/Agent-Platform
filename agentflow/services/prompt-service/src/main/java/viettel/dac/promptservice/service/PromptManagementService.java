package viettel.dac.promptservice.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import viettel.dac.promptservice.dto.request.PromptTemplateRequest;
import viettel.dac.promptservice.dto.search.PromptTemplateSearchCriteria;
import viettel.dac.promptservice.exception.ResourceAlreadyExistsException;
import viettel.dac.promptservice.exception.ResourceNotFoundException;
import viettel.dac.promptservice.exception.ValidationException;
import viettel.dac.promptservice.model.entity.PromptTemplate;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing prompt templates.
 * Provides core functionality for creating, retrieving, updating, and deleting templates,
 * with integrated caching and Elasticsearch indexing.
 */
public interface PromptManagementService {

    /**
     * Create a new prompt template.
     *
     * @param request The template creation request
     * @return The created template
     * @throws ResourceAlreadyExistsException if a template with the same name exists in the project
     * @throws ValidationException if the request contains invalid data
     */
    PromptTemplate createTemplate(PromptTemplateRequest request);

    /**
     * Get a template by ID.
     *
     * @param id The template ID
     * @return Optional containing the template if found, empty otherwise
     */
    Optional<PromptTemplate> getTemplateById(String id);

    /**
     * Get a template by ID with all versions loaded.
     *
     * @param id The template ID
     * @return Optional containing the template with versions if found, empty otherwise
     */
    Optional<PromptTemplate> getTemplateWithVersions(String id);

    /**
     * Get templates by project ID.
     *
     * @param projectId The project ID
     * @param pageable Pagination parameters
     * @return Page of templates belonging to the project
     */
    Page<PromptTemplate> getTemplatesByProject(String projectId, Pageable pageable);

    /**
     * Get templates by category.
     *
     * @param category The category name
     * @param pageable Pagination parameters
     * @return Page of templates in the specified category
     */
    Page<PromptTemplate> getTemplatesByCategory(String category, Pageable pageable);

    /**
     * Search templates based on various criteria.
     *
     * @param criteria The search criteria
     * @param pageable Pagination parameters
     * @return Page of templates matching the criteria
     */
    Page<PromptTemplate> searchTemplates(PromptTemplateSearchCriteria criteria, Pageable pageable);

    /**
     * Update an existing template.
     *
     * @param id The template ID
     * @param request The template update request
     * @return The updated template
     * @throws ResourceNotFoundException if the template is not found
     * @throws ResourceAlreadyExistsException if name is changed and already exists
     * @throws ValidationException if the request contains invalid data
     */
    PromptTemplate updateTemplate(String id, PromptTemplateRequest request);

    /**
     * Delete a template.
     *
     * @param id The template ID
     * @throws ResourceNotFoundException if the template is not found
     */
    void deleteTemplate(String id);

    /**
     * Get distinct categories from all templates.
     *
     * @return List of unique categories
     */
    List<String> getAllCategories();
}