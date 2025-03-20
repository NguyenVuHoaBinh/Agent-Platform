package viettel.dac.promptservice.repository.jpa;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import viettel.dac.promptservice.model.entity.PromptTemplate;

import java.util.List;
import java.util.Optional;

/**
 * Repository for prompt templates
 */
@Repository
public interface PromptTemplateRepository extends JpaRepository<PromptTemplate, String> {

    @Override
    @Cacheable(value = "promptTemplates", key = "#id")
    Optional<PromptTemplate> findById(String id);

    @Override
    @CacheEvict(value = "promptTemplates", key = "#entity.id")
    <S extends PromptTemplate> S save(S entity);

    @Override
    @CacheEvict(value = "promptTemplates", key = "#id")
    void deleteById(String id);

    Page<PromptTemplate> findByProjectId(String projectId, Pageable pageable);

    List<PromptTemplate> findByProjectId(String projectId);

    Page<PromptTemplate> findByCategoryOrderByUpdatedAtDesc(String category, Pageable pageable);

    @Query("SELECT t FROM PromptTemplate t WHERE " +
            "(:searchTerm IS NULL OR LOWER(t.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(t.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) AND " +
            "(:projectId IS NULL OR t.projectId = :projectId) AND " +
            "(:category IS NULL OR t.category = :category)")
    Page<PromptTemplate> search(
            @Param("searchTerm") String searchTerm,
            @Param("projectId") String projectId,
            @Param("category") String category,
            Pageable pageable);

    boolean existsByNameAndProjectId(String name, String projectId);
}