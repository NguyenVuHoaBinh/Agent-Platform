package viettel.dac.promptservice.repository.jpa;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import viettel.dac.promptservice.model.entity.PromptTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Enhanced repository for prompt templates
 */
@Repository
public interface PromptTemplateRepository extends JpaRepository<PromptTemplate, String> {

    @Override
    @Cacheable(value = "promptTemplates", key = "#id")
    Optional<PromptTemplate> findById(String id);

    @Override
    @CachePut(value = "promptTemplates", key = "#entity.id")
    <S extends PromptTemplate> S save(S entity);

    @Override
    @CacheEvict(value = "promptTemplates", key = "#id")
    void deleteById(String id);

    /**
     * Find template by ID with versions loaded
     */
    @Query("SELECT t FROM PromptTemplate t LEFT JOIN FETCH t.versions WHERE t.id = :id")
    @Cacheable(value = "promptTemplates", key = "'withVersions_' + #id")
    Optional<PromptTemplate> findByIdWithVersions(@Param("id") String id);

    /**
     * Find templates by project ID
     */
    @Cacheable(value = "promptTemplatesByProject", key = "#projectId")
    Page<PromptTemplate> findByProjectId(String projectId, Pageable pageable);

    /**
     * Get all templates by project ID (non-paginated)
     */
    @Cacheable(value = "promptTemplatesByProject", key = "'list_' + #projectId")
    List<PromptTemplate> findByProjectId(String projectId);

    /**
     * Find templates by category
     */
    @Cacheable(value = "promptTemplatesByCategory", key = "#category + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    Page<PromptTemplate> findByCategoryOrderByUpdatedAtDesc(String category, Pageable pageable);

    /**
     * Find templates by search term
     */
    @Query("SELECT t FROM PromptTemplate t WHERE " +
            "(:searchTerm IS NULL OR LOWER(t.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(t.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) AND " +
            "(:projectId IS NULL OR t.projectId = :projectId) AND " +
            "(:category IS NULL OR t.category = :category)")
    @Cacheable(value = "promptTemplatesSearch", key = "#searchTerm + '_' + #projectId + '_' + #category + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    Page<PromptTemplate> search(
            @Param("searchTerm") String searchTerm,
            @Param("projectId") String projectId,
            @Param("category") String category,
            Pageable pageable);

    /**
     * Check if template name exists in project
     */
    boolean existsByNameAndProjectId(String name, String projectId);

    /**
     * Find templates by creator
     */
    @Cacheable(value = "promptTemplatesByCreator", key = "#createdBy + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    Page<PromptTemplate> findByCreatedBy(String createdBy, Pageable pageable);

    /**
     * Find templates created after a specific date
     */
    Page<PromptTemplate> findByCreatedAtAfter(LocalDateTime createdAt, Pageable pageable);

    /**
     * Find templates updated after a specific date
     */
    Page<PromptTemplate> findByUpdatedAtAfter(LocalDateTime updatedAt, Pageable pageable);

    /**
     * Find templates by multiple categories
     */
    @Query("SELECT t FROM PromptTemplate t WHERE t.category IN :categories")
    Page<PromptTemplate> findByCategoryIn(@Param("categories") Set<String> categories, Pageable pageable);

    /**
     * Get distinct categories
     */
    @Query("SELECT DISTINCT t.category FROM PromptTemplate t WHERE t.category IS NOT NULL")
    @Cacheable(value = "promptTemplateCategories")
    List<String> findDistinctCategories();

    /**
     * Count templates by project
     */
    @Cacheable(value = "promptTemplateCountByProject", key = "#projectId")
    long countByProjectId(String projectId);

    /**
     * Count templates with published versions
     */
    @Query("SELECT COUNT(DISTINCT t) FROM PromptTemplate t JOIN t.versions v WHERE v.status = 'PUBLISHED'")
    long countWithPublishedVersions();
}