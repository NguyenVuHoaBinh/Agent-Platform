package viettel.dac.promptservice.repository.mock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.RefreshPolicy;
import org.springframework.stereotype.Repository;
import viettel.dac.promptservice.model.document.PromptTemplateDocument;
import viettel.dac.promptservice.repository.elasticsearch.PromptTemplateSearchRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Mock implementation of PromptTemplateSearchRepository for tests
 */
@Repository
@Profile("test")
@Primary
@ConditionalOnProperty(name = "spring.elasticsearch.enabled", havingValue = "false")
public class MockPromptTemplateSearchRepository implements PromptTemplateSearchRepository {

    @Override
    public Page<PromptTemplateDocument> findByNameContainingOrDescriptionContaining(
            String name, String description, Pageable pageable) {
        return new PageImpl<>(new ArrayList<>(), pageable, 0);
    }

    @Override
    public Page<PromptTemplateDocument> findByProjectId(String projectId, Pageable pageable) {
        return new PageImpl<>(new ArrayList<>(), pageable, 0);
    }

    @Override
    public Page<PromptTemplateDocument> findByCategory(String category, Pageable pageable) {
        return new PageImpl<>(new ArrayList<>(), pageable, 0);
    }

    @Override
    public Page<PromptTemplateDocument> findByCreatedBy(String createdBy, Pageable pageable) {
        return new PageImpl<>(new ArrayList<>(), pageable, 0);
    }

    @Override
    public Page<PromptTemplateDocument> findByHasPublishedVersionTrue(Pageable pageable) {
        return new PageImpl<>(new ArrayList<>(), pageable, 0);
    }

    @Override
    public Page<PromptTemplateDocument> findByMinimumVersionCount(int minCount, Pageable pageable) {
        return new PageImpl<>(new ArrayList<>(), pageable, 0);
    }

    @Override
    public Page<PromptTemplateDocument> findByCategoryIn(List<String> categories, Pageable pageable) {
        return new PageImpl<>(new ArrayList<>(), pageable, 0);
    }

    @Override
    public List<String> findDistinctCategories() {
        return new ArrayList<>();
    }

    // ElasticsearchRepository implementation
    @Override
    public <S extends PromptTemplateDocument> S save(S entity) {
        return entity;
    }

    @Override
    public <S extends PromptTemplateDocument> S save(S entity, RefreshPolicy refreshPolicy) {
        return entity;
    }

    @Override
    public <S extends PromptTemplateDocument> Iterable<S> saveAll(Iterable<S> entities) {
        return entities;
    }

    @Override
    public <S extends PromptTemplateDocument> Iterable<S> saveAll(Iterable<S> entities, RefreshPolicy refreshPolicy) {
        return entities;
    }

    @Override
    public Optional<PromptTemplateDocument> findById(String id) {
        return Optional.empty();
    }

    @Override
    public boolean existsById(String id) {
        return false;
    }

    @Override
    public Iterable<PromptTemplateDocument> findAll() {
        return new ArrayList<>();
    }

    @Override
    public Iterable<PromptTemplateDocument> findAll(Sort sort) {
        return new ArrayList<>();
    }

    @Override
    public Iterable<PromptTemplateDocument> findAllById(Iterable<String> ids) {
        return new ArrayList<>();
    }

    @Override
    public long count() {
        return 0;
    }

    @Override
    public void deleteById(String id) {
        // Do nothing
    }

    @Override
    public void deleteById(String id, RefreshPolicy refreshPolicy) {
        // Do nothing
    }

    @Override
    public void delete(PromptTemplateDocument entity) {
        // Do nothing
    }

    @Override
    public void delete(PromptTemplateDocument entity, RefreshPolicy refreshPolicy) {
        // Do nothing
    }

    @Override
    public void deleteAllById(Iterable<? extends String> ids) {
        // Do nothing
    }

    @Override
    public void deleteAllById(Iterable<? extends String> ids, RefreshPolicy refreshPolicy) {
        // Do nothing
    }

    @Override
    public void deleteAll(Iterable<? extends PromptTemplateDocument> entities) {
        // Do nothing
    }

    @Override
    public void deleteAll(Iterable<? extends PromptTemplateDocument> entities, RefreshPolicy refreshPolicy) {
        // Do nothing
    }

    @Override
    public void deleteAll() {
        // Do nothing
    }

    @Override
    public void deleteAll(RefreshPolicy refreshPolicy) {
        // Do nothing
    }

    @Override
    public Page<PromptTemplateDocument> findAll(Pageable pageable) {
        return new PageImpl<>(new ArrayList<>(), pageable, 0);
    }

    @Override
    public Page<PromptTemplateDocument> searchSimilar(PromptTemplateDocument entity, String[] fields, Pageable pageable) {
        return new PageImpl<>(new ArrayList<>(), pageable, 0);
    }
} 