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
import viettel.dac.promptservice.model.document.PromptExecutionDocument;
import viettel.dac.promptservice.repository.elasticsearch.PromptExecutionSearchRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Mock implementation of PromptExecutionSearchRepository for tests
 */
@Repository
@Profile("test")
@Primary
@ConditionalOnProperty(name = "spring.elasticsearch.enabled", havingValue = "false")
public class MockPromptExecutionSearchRepository implements PromptExecutionSearchRepository {

    @Override
    public Page<PromptExecutionDocument> findByVersionId(String versionId, Pageable pageable) {
        return new PageImpl<>(new ArrayList<>(), pageable, 0);
    }

    @Override
    public Page<PromptExecutionDocument> findByTemplateId(String templateId, Pageable pageable) {
        return new PageImpl<>(new ArrayList<>(), pageable, 0);
    }

    @Override
    public Page<PromptExecutionDocument> findByProviderId(String providerId, Pageable pageable) {
        return new PageImpl<>(new ArrayList<>(), pageable, 0);
    }

    @Override
    public Page<PromptExecutionDocument> findByModelId(String modelId, Pageable pageable) {
        return new PageImpl<>(new ArrayList<>(), pageable, 0);
    }

    @Override
    public Page<PromptExecutionDocument> findByExecutedBy(String executedBy, Pageable pageable) {
        return new PageImpl<>(new ArrayList<>(), pageable, 0);
    }

    @Override
    public Page<PromptExecutionDocument> findByExecutedAtBetween(LocalDateTime start, LocalDateTime end, Pageable pageable) {
        return new PageImpl<>(new ArrayList<>(), pageable, 0);
    }

    @Override
    public Page<PromptExecutionDocument> findBySuccessfulTrue(Pageable pageable) {
        return new PageImpl<>(new ArrayList<>(), pageable, 0);
    }

    @Override
    public Page<PromptExecutionDocument> findBySuccessfulFalse(Pageable pageable) {
        return new PageImpl<>(new ArrayList<>(), pageable, 0);
    }

    @Override
    public Map<String, Object> getExecutionsPerDay(String templateId) {
        Map<String, Object> result = new HashMap<>();
        result.put("aggregations", new HashMap<>());
        return result;
    }

    @Override
    public Map<String, Object> getSuccessRate(String templateId) {
        Map<String, Object> result = new HashMap<>();
        result.put("aggregations", new HashMap<>());
        return result;
    }

    @Override
    public Map<String, Object> getAverageResponseTime(String templateId) {
        Map<String, Object> result = new HashMap<>();
        result.put("aggregations", new HashMap<>());
        return result;
    }

    @Override
    public Map<String, Object> getAverageTokenCount(String templateId) {
        Map<String, Object> result = new HashMap<>();
        result.put("aggregations", new HashMap<>());
        return result;
    }

    // ElasticsearchRepository implementation
    @Override
    public <S extends PromptExecutionDocument> S save(S entity) {
        return entity;
    }

    @Override
    public <S extends PromptExecutionDocument> S save(S entity, RefreshPolicy refreshPolicy) {
        return entity;
    }

    @Override
    public <S extends PromptExecutionDocument> Iterable<S> saveAll(Iterable<S> entities) {
        return entities;
    }

    @Override
    public <S extends PromptExecutionDocument> Iterable<S> saveAll(Iterable<S> entities, RefreshPolicy refreshPolicy) {
        return entities;
    }

    @Override
    public Optional<PromptExecutionDocument> findById(String id) {
        return Optional.empty();
    }

    @Override
    public boolean existsById(String id) {
        return false;
    }

    @Override
    public Iterable<PromptExecutionDocument> findAll() {
        return new ArrayList<>();
    }

    @Override
    public Iterable<PromptExecutionDocument> findAll(Sort sort) {
        return new ArrayList<>();
    }

    @Override
    public Iterable<PromptExecutionDocument> findAllById(Iterable<String> ids) {
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
    public void delete(PromptExecutionDocument entity) {
        // Do nothing
    }

    @Override
    public void delete(PromptExecutionDocument entity, RefreshPolicy refreshPolicy) {
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
    public void deleteAll(Iterable<? extends PromptExecutionDocument> entities) {
        // Do nothing
    }

    @Override
    public void deleteAll(Iterable<? extends PromptExecutionDocument> entities, RefreshPolicy refreshPolicy) {
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
    public Page<PromptExecutionDocument> findAll(Pageable pageable) {
        return new PageImpl<>(new ArrayList<>(), pageable, 0);
    }

    @Override
    public Page<PromptExecutionDocument> searchSimilar(PromptExecutionDocument entity, String[] fields, Pageable pageable) {
        return new PageImpl<>(new ArrayList<>(), pageable, 0);
    }
} 