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
import viettel.dac.promptservice.model.document.PromptVersionDocument;
import viettel.dac.promptservice.repository.elasticsearch.PromptVersionSearchRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Mock implementation of PromptVersionSearchRepository for tests
 */
@Repository
@Profile("test")
@Primary
@ConditionalOnProperty(name = "spring.elasticsearch.enabled", havingValue = "false")
public class MockPromptVersionSearchRepository implements PromptVersionSearchRepository {

    @Override
    public Page<PromptVersionDocument> findByTemplateId(String templateId, Pageable pageable) {
        return new PageImpl<>(new ArrayList<>(), pageable, 0);
    }

    @Override
    public Page<PromptVersionDocument> findByContentContaining(String content, Pageable pageable) {
        return new PageImpl<>(new ArrayList<>(), pageable, 0);
    }

    @Override
    public Page<PromptVersionDocument> findByStatus(String status, Pageable pageable) {
        return new PageImpl<>(new ArrayList<>(), pageable, 0);
    }

    @Override
    public Page<PromptVersionDocument> findByStatusIn(List<String> statuses, Pageable pageable) {
        return new PageImpl<>(new ArrayList<>(), pageable, 0);
    }

    @Override
    public Page<PromptVersionDocument> findByVersionNumber(String versionNumber, Pageable pageable) {
        return new PageImpl<>(new ArrayList<>(), pageable, 0);
    }

    @Override
    public Page<PromptVersionDocument> findByParameterNamesContaining(String parameterName, Pageable pageable) {
        return new PageImpl<>(new ArrayList<>(), pageable, 0);
    }

    @Override
    public PromptVersionDocument findLatestVersionForTemplate(String templateId) {
        return null;
    }

    @Override
    public PromptVersionDocument findLatestPublishedVersionForTemplate(String templateId) {
        return null;
    }

    // ElasticsearchRepository implementation
    @Override
    public <S extends PromptVersionDocument> S save(S entity) {
        return entity;
    }

    @Override
    public <S extends PromptVersionDocument> S save(S entity, RefreshPolicy refreshPolicy) {
        return entity;
    }

    @Override
    public <S extends PromptVersionDocument> Iterable<S> saveAll(Iterable<S> entities) {
        return entities;
    }

    @Override
    public <S extends PromptVersionDocument> Iterable<S> saveAll(Iterable<S> entities, RefreshPolicy refreshPolicy) {
        return entities;
    }

    @Override
    public Optional<PromptVersionDocument> findById(String id) {
        return Optional.empty();
    }

    @Override
    public boolean existsById(String id) {
        return false;
    }

    @Override
    public Iterable<PromptVersionDocument> findAll() {
        return new ArrayList<>();
    }

    @Override
    public Iterable<PromptVersionDocument> findAll(Sort sort) {
        return new ArrayList<>();
    }

    @Override
    public Iterable<PromptVersionDocument> findAllById(Iterable<String> ids) {
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
    public void delete(PromptVersionDocument entity) {
        // Do nothing
    }

    @Override
    public void delete(PromptVersionDocument entity, RefreshPolicy refreshPolicy) {
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
    public void deleteAll(Iterable<? extends PromptVersionDocument> entities) {
        // Do nothing
    }

    @Override
    public void deleteAll(Iterable<? extends PromptVersionDocument> entities, RefreshPolicy refreshPolicy) {
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
    public Page<PromptVersionDocument> findAll(Pageable pageable) {
        return new PageImpl<>(new ArrayList<>(), pageable, 0);
    }

    @Override
    public Page<PromptVersionDocument> searchSimilar(PromptVersionDocument entity, String[] fields, Pageable pageable) {
        return new PageImpl<>(new ArrayList<>(), pageable, 0);
    }
} 