package viettel.dac.promptservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import viettel.dac.promptservice.dto.request.PromptTemplateRequest;
import viettel.dac.promptservice.dto.search.PromptTemplateSearchCriteria;
import viettel.dac.promptservice.exception.ResourceAlreadyExistsException;
import viettel.dac.promptservice.exception.ResourceNotFoundException;
import viettel.dac.promptservice.exception.ValidationException;
import viettel.dac.promptservice.model.entity.PromptTemplate;
import viettel.dac.promptservice.repository.jpa.PromptTemplateRepository;
import viettel.dac.promptservice.repository.jpa.PromptVersionRepository;
import viettel.dac.promptservice.security.SecurityUtils;
import viettel.dac.promptservice.service.PromptManagementService;
import viettel.dac.promptservice.service.elasticsearch.PromptSearchService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromptManagementServiceImpl implements PromptManagementService {

    private final PromptTemplateRepository templateRepository;
    private final PromptVersionRepository versionRepository;
    private final SecurityUtils securityUtils;
    private final PromptSearchService searchService;

    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9\\s_-]+$");

    @Override
    @Transactional
    @CachePut(value = "promptTemplates", key = "#result.id")
    public PromptTemplate createTemplate(PromptTemplateRequest request) {
        log.debug("Creating new prompt template: {}", request.getName());

        // Validate request
        validateTemplateRequest(request);

        // Check if template name exists in project
        if (templateRepository.existsByNameAndProjectId(request.getName(), request.getProjectId())) {
            throw new ResourceAlreadyExistsException("Template name already exists in project: " + request.getName());
        }

        // Get current user
        String currentUserId = securityUtils.getCurrentUserId()
                .orElse("system"); // Fallback for tests or batch operations

        // Create template entity
        PromptTemplate template = PromptTemplate.builder()
                .name(request.getName())
                .description(request.getDescription())
                .projectId(request.getProjectId())
                .category(request.getCategory())
                .createdBy(currentUserId)
                .build();

        // Save template
        PromptTemplate savedTemplate = templateRepository.save(template);

        // Index in Elasticsearch
        try {
            searchService.indexPromptTemplate(savedTemplate);
        } catch (Exception e) {
            // Log error but don't fail the operation if indexing fails
            log.error("Failed to index template in Elasticsearch: {}", e.getMessage(), e);
        }

        log.info("Created prompt template with ID: {}", savedTemplate.getId());
        return savedTemplate;
    }

    @Override
    @Cacheable(value = "promptTemplates", key = "#id", unless = "#result == null")
    public Optional<PromptTemplate> getTemplateById(String id) {
        log.debug("Fetching prompt template with ID: {}", id);
        return templateRepository.findById(id);
    }

    @Override
    @Cacheable(value = "promptTemplatesWithVersions", key = "#id", unless = "#result == null")
    public Optional<PromptTemplate> getTemplateWithVersions(String id) {
        log.debug("Fetching prompt template with versions, ID: {}", id);
        return templateRepository.findByIdWithVersions(id);
    }

    @Override
    @Cacheable(value = "promptTemplatesByProject",
            key = "#projectId + '_' + #pageable.pageNumber + '_' + #pageable.pageSize",
            unless = "#result.totalElements == 0")
    public Page<PromptTemplate> getTemplatesByProject(String projectId, Pageable pageable) {
        log.debug("Fetching templates for project: {}", projectId);
        return templateRepository.findByProjectId(projectId, pageable);
    }

    @Override
    @Cacheable(value = "promptTemplatesByCategory",
            key = "#category + '_' + #pageable.pageNumber + '_' + #pageable.pageSize",
            unless = "#result.totalElements == 0")
    public Page<PromptTemplate> getTemplatesByCategory(String category, Pageable pageable) {
        log.debug("Fetching templates by category: {}", category);
        return templateRepository.findByCategoryOrderByUpdatedAtDesc(category, pageable);
    }

    @Override
    @Cacheable(value = "promptTemplatesSearch",
            key = "#criteria.hashCode() + '_' + #pageable.pageNumber + '_' + #pageable.pageSize",
            unless = "#result.totalElements == 0")
    public Page<PromptTemplate> searchTemplates(PromptTemplateSearchCriteria criteria, Pageable pageable) {
        log.debug("Searching templates with criteria: {}", criteria);

        // If using Elasticsearch and criteria supports it, delegate to search service
        if (shouldUseElasticsearch(criteria)) {
            try {
                return searchService.searchTemplates(criteria, pageable);
            } catch (Exception e) {
                log.warn("Elasticsearch search failed, falling back to database: {}", e.getMessage());
                // Fall back to database search if Elasticsearch fails
            }
        }

        // Database search
        return templateRepository.search(
                criteria.getSearchText(),
                criteria.getProjectId(),
                criteria.getCategory(),
                pageable
        );
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    @Caching(
            put = {
                    @CachePut(value = "promptTemplates", key = "#result.id")
            },
            evict = {
                    @CacheEvict(value = "promptTemplatesByProject", key = "#result.projectId + '_*'"),
                    @CacheEvict(value = "promptTemplatesByCategory", key = "#result.category + '_*'", condition = "#result.category != null"),
                    @CacheEvict(value = "promptTemplatesWithVersions", key = "#id"),
                    @CacheEvict(value = "promptTemplatesSearch", allEntries = true)
            }
    )
    public PromptTemplate updateTemplate(String id, PromptTemplateRequest request) {
        log.debug("Updating prompt template with ID: {}", id);

        // Validate request
        validateTemplateRequest(request);

        // Get template by ID
        PromptTemplate existingTemplate = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found with id: " + id));

        // Check if name is changed and already exists in project
        if (!existingTemplate.getName().equals(request.getName()) &&
                templateRepository.existsByNameAndProjectId(request.getName(), existingTemplate.getProjectId())) {
            throw new ResourceAlreadyExistsException("Template name already exists in project: " + request.getName());
        }

        // Check if template has published versions that would be affected
        boolean hasPublishedVersions = existingTemplate.hasPublishedVersion();

        // Store old category for cache invalidation if changed
        String oldCategory = existingTemplate.getCategory();

        // Update template fields
        existingTemplate.setName(request.getName());
        existingTemplate.setDescription(request.getDescription());
        existingTemplate.setCategory(request.getCategory());

        // Save updated template
        PromptTemplate updatedTemplate = templateRepository.save(existingTemplate);

        // Index in Elasticsearch
        try {
            searchService.indexPromptTemplate(updatedTemplate);
        } catch (Exception e) {
            log.error("Failed to update template in Elasticsearch: {}", e.getMessage(), e);
            // Continue process despite indexing failure
        }

        // Invalidate additional caches if category changed
        if (oldCategory != null && !oldCategory.equals(request.getCategory())) {
            // This would be handled by the @CacheEvict annotations above
            log.debug("Category changed from {} to {}, cache entries invalidated",
                    oldCategory, request.getCategory());
        }

        log.info("Updated prompt template with ID: {}", updatedTemplate.getId());
        return updatedTemplate;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    @Caching(evict = {
            @CacheEvict(value = "promptTemplates", key = "#id"),
            @CacheEvict(value = "promptTemplatesByProject", allEntries = true),
            @CacheEvict(value = "promptTemplatesByCategory", allEntries = true),
            @CacheEvict(value = "promptTemplatesWithVersions", key = "#id"),
            @CacheEvict(value = "promptTemplatesSearch", allEntries = true),
            @CacheEvict(value = "promptTemplateCategories", allEntries = true)
    })
    public void deleteTemplate(String id) {
        log.debug("Deleting prompt template with ID: {}", id);

        PromptTemplate template = templateRepository.findByIdWithVersions(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found with id: " + id));

        // Check for published versions
        boolean hasPublishedVersions = template.hasPublishedVersion();

        if (hasPublishedVersions) {
            log.warn("Deleting template with published versions: {}", id);
            // Additional business logic could be added here
            // (e.g., requiring special permissions for deletion)
        }

        // Delete from database (cascading delete will handle related entities)
        templateRepository.delete(template);

        // Delete from Elasticsearch
        try {
            searchService.deleteTemplateIndex(id);
        } catch (Exception e) {
            log.error("Failed to delete template from Elasticsearch: {}", e.getMessage(), e);
            // Continue process despite indexing failure
        }

        log.info("Deleted prompt template with ID: {}", id);
    }

    @Override
    @Cacheable(value = "promptTemplateCategories")
    public List<String> getAllCategories() {
        log.debug("Fetching all template categories");
        return templateRepository.findDistinctCategories();
    }

    /**
     * Validate template request data
     *
     * @param request The template request to validate
     * @throws ValidationException if validation fails
     */
    private void validateTemplateRequest(PromptTemplateRequest request) {
        Map<String, String> errors = new HashMap<>();

        // Check name pattern
        if (!NAME_PATTERN.matcher(request.getName()).matches()) {
            errors.put("name", "Template name must contain only letters, numbers, spaces, hyphens, and underscores");
        }

        // Check project ID
        if (!StringUtils.hasText(request.getProjectId())) {
            errors.put("projectId", "Project ID is required");
        }

        // Additional validations can be added as needed

        if (!errors.isEmpty()) {
            throw new ValidationException("Template validation failed", errors);
        }
    }

    /**
     * Determine if search should use Elasticsearch based on criteria
     */
    private boolean shouldUseElasticsearch(PromptTemplateSearchCriteria criteria) {
        // Use Elasticsearch for text search or advanced filtering
        return criteria.getSearchText() != null ||
                criteria.getFromDate() != null ||
                criteria.getToDate() != null ||
                criteria.getHasPublishedVersion() != null ||
                criteria.getMinVersionCount() != null ||
                (criteria.getCategories() != null && !criteria.getCategories().isEmpty());
    }
}