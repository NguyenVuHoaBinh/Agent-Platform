package viettel.dac.promptservice.service.elasticsearch;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.json.JsonData;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import viettel.dac.promptservice.dto.search.PromptExecutionSearchCriteria;
import viettel.dac.promptservice.dto.search.PromptTemplateSearchCriteria;
import viettel.dac.promptservice.dto.search.PromptVersionSearchCriteria;
import viettel.dac.promptservice.model.enums.VersionStatus;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Builder for Elasticsearch queries using Elasticsearch Java API Client 8.17.3
 */
@Component
@RequiredArgsConstructor
public class ElasticsearchQueryBuilder {

    /**
     * Build a search request for prompt templates.
     */
    public SearchRequest buildTemplateSearchRequest(PromptTemplateSearchCriteria criteria, Pageable pageable, String indexName) {
        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();

        // Text search on "name" and "description"
        if (StringUtils.hasText(criteria.getSearchText())) {
            if (Boolean.TRUE.equals(criteria.getUseExactMatch())) {
                MultiMatchQuery multiMatchQuery = MultiMatchQuery.of(m -> m
                        .fields("name", "description")
                        .query(criteria.getSearchText())
                        .type(TextQueryType.Phrase));
                boolBuilder.must(q -> q.multiMatch(multiMatchQuery));
            } else if (Boolean.TRUE.equals(criteria.getUseFuzzyMatch())) {
                MultiMatchQuery multiMatchQuery = MultiMatchQuery.of(m -> m
                        .fields("name", "description")
                        .query(criteria.getSearchText())
                        .fuzziness("AUTO"));
                boolBuilder.must(q -> q.multiMatch(multiMatchQuery));
            } else {
                MultiMatchQuery multiMatchQuery = MultiMatchQuery.of(m -> m
                        .fields("name", "description")
                        .query(criteria.getSearchText()));
                boolBuilder.must(q -> q.multiMatch(multiMatchQuery));
            }
        }

        // Filter by project
        if (StringUtils.hasText(criteria.getProjectId())) {
            boolBuilder.filter(q -> q.term(t -> t.field("projectId")
                    .value(FieldValue.of(criteria.getProjectId()))));
        }

        // Filter by category
        if (StringUtils.hasText(criteria.getCategory())) {
            boolBuilder.filter(q -> q.term(t -> t.field("category")
                    .value(FieldValue.of(criteria.getCategory()))));
        }

        // Filter by multiple categories
        if (criteria.getCategories() != null && !criteria.getCategories().isEmpty()) {
            boolBuilder.filter(q -> q.terms(t -> t.field("category")
                    .terms(terms -> terms.value(
                            criteria.getCategories().stream()
                                    .map(FieldValue::of)
                                    .collect(Collectors.toList())
                    ))));
        }

        // Filter by creator
        if (StringUtils.hasText(criteria.getCreatedBy())) {
            boolBuilder.filter(q -> q.term(t -> t.field("createdBy")
                    .value(FieldValue.of(criteria.getCreatedBy()))));
        }

        // Date range filter for createdAt using inline lambda with conditional logic
        if (criteria.getFromDate() != null || criteria.getToDate() != null) {
            boolBuilder.filter(q -> q.range(r -> r.untyped(n -> {
                var builder = n.field("createdAt");
                if (criteria.getFromDate() != null) {
                    builder = builder.gte(JsonData.of(criteria.getFromDate()));
                }
                if (criteria.getToDate() != null) {
                    builder = builder.lte(JsonData.of(criteria.getToDate()));
                }
                return builder;
            })));
        }

        // Has published version filter
        if (criteria.getHasPublishedVersion() != null) {
            boolBuilder.filter(q -> q.term(t -> t.field("hasPublishedVersion")
                    .value(FieldValue.of(criteria.getHasPublishedVersion()))));
        }

        // Version count filter (gte) using inline lambda with untyped builder
        if (criteria.getMinVersionCount() != null) {
            boolBuilder.filter(q -> q.range(r -> r.untyped(n ->
                    n.field("versionCount").gte(JsonData.of(criteria.getMinVersionCount()))
            )));
        }

        return new SearchRequest.Builder()
                .index(indexName)
                .query(q -> q.bool(boolBuilder.build()))
                .from(pageable.getPageNumber() * pageable.getPageSize())
                .size(pageable.getPageSize())
                .minScore(criteria.getMinScore() != null ? criteria.getMinScore().doubleValue() : null)
                .build();
    }

    /**
     * Build a search request for prompt versions.
     */
    public SearchRequest buildVersionSearchRequest(PromptVersionSearchCriteria criteria, Pageable pageable, String indexName) {
        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();

        // Text search on "content"
        if (StringUtils.hasText(criteria.getSearchText())) {
            if (Boolean.TRUE.equals(criteria.getUseExactMatch())) {
                MatchPhraseQuery matchPhraseQuery = MatchPhraseQuery.of(m -> m
                        .field("content")
                        .query(criteria.getSearchText()));
                boolBuilder.must(q -> q.matchPhrase(matchPhraseQuery));
            } else if (Boolean.TRUE.equals(criteria.getUseFuzzyMatch())) {
                MatchQuery matchQuery = MatchQuery.of(m -> m
                        .field("content")
                        .query(criteria.getSearchText())
                        .fuzziness("AUTO"));
                boolBuilder.must(q -> q.match(matchQuery));
            } else {
                MatchQuery matchQuery = MatchQuery.of(m -> m
                        .field("content")
                        .query(criteria.getSearchText()));
                boolBuilder.must(q -> q.match(matchQuery));
            }
        }

        // Filter by templateId
        if (StringUtils.hasText(criteria.getTemplateId())) {
            boolBuilder.filter(q -> q.term(t -> t.field("templateId")
                    .value(FieldValue.of(criteria.getTemplateId()))));
        }

        // Filter by statuses
        if (criteria.getStatuses() != null && !criteria.getStatuses().isEmpty()) {
            List<String> statusNames = criteria.getStatuses().stream()
                    .map(VersionStatus::name)
                    .toList();
            boolBuilder.filter(q -> q.terms(t -> t.field("status")
                    .terms(terms -> terms.value(
                            statusNames.stream()
                                    .map(FieldValue::of)
                                    .collect(Collectors.toList())
                    ))));
        }

        // Filter by creator
        if (StringUtils.hasText(criteria.getCreatedBy())) {
            boolBuilder.filter(q -> q.term(t -> t.field("createdBy")
                    .value(FieldValue.of(criteria.getCreatedBy()))));
        }

        // Filter by version number
        if (StringUtils.hasText(criteria.getVersionNumber())) {
            boolBuilder.filter(q -> q.term(t -> t.field("versionNumber")
                    .value(FieldValue.of(criteria.getVersionNumber()))));
        }

        // Filter by parameter name
        if (StringUtils.hasText(criteria.getParameterName())) {
            boolBuilder.filter(q -> q.term(t -> t.field("parameterNames")
                    .value(FieldValue.of(criteria.getParameterName()))));
        }

        // Version range filters on majorVersion (using a single range query for min and/or max)
        if (criteria.getMinMajorVersion() != null || criteria.getMaxMajorVersion() != null) {
            boolBuilder.filter(q -> q.range(r -> r.untyped(n -> {
                var builder = n.field("majorVersion");
                if (criteria.getMinMajorVersion() != null) {
                    builder = builder.gte(JsonData.of(criteria.getMinMajorVersion()));
                }
                if (criteria.getMaxMajorVersion() != null) {
                    builder = builder.lte(JsonData.of(criteria.getMaxMajorVersion()));
                }
                return builder;
            })));
        }

        return new SearchRequest.Builder()
                .index(indexName)
                .query(q -> q.bool(boolBuilder.build()))
                .from(pageable.getPageNumber() * pageable.getPageSize())
                .size(pageable.getPageSize())
                .minScore(criteria.getMinScore() != null ? criteria.getMinScore().doubleValue() : null)
                .build();
    }

    /**
     * Build a search request for prompt executions.
     */
    public SearchRequest buildExecutionSearchRequest(PromptExecutionSearchCriteria criteria, Pageable pageable, String indexName) {
        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();

        // Filter by versionId
        if (StringUtils.hasText(criteria.getVersionId())) {
            boolBuilder.filter(q -> q.term(t -> t.field("versionId")
                    .value(FieldValue.of(criteria.getVersionId()))));
        }

        // Filter by templateId
        if (StringUtils.hasText(criteria.getTemplateId())) {
            boolBuilder.filter(q -> q.term(t -> t.field("templateId")
                    .value(FieldValue.of(criteria.getTemplateId()))));
        }

        // Filter by providerId
        if (StringUtils.hasText(criteria.getProviderId())) {
            boolBuilder.filter(q -> q.term(t -> t.field("providerId")
                    .value(FieldValue.of(criteria.getProviderId()))));
        }

        // Filter by modelId
        if (StringUtils.hasText(criteria.getModelId())) {
            boolBuilder.filter(q -> q.term(t -> t.field("modelId")
                    .value(FieldValue.of(criteria.getModelId()))));
        }

        // Filter by executedBy
        if (StringUtils.hasText(criteria.getExecutedBy())) {
            boolBuilder.filter(q -> q.term(t -> t.field("executedBy")
                    .value(FieldValue.of(criteria.getExecutedBy()))));
        }

        if (criteria.getFromDate() != null || criteria.getToDate() != null) {
            boolBuilder.filter(q -> q.range(r -> r.untyped(n -> {
                var builder = n.field("executedAt");
                if (criteria.getFromDate() != null) {
                    builder = builder.gte(JsonData.of(criteria.getFromDate()));
                }
                if (criteria.getToDate() != null) {
                    builder = builder.lte(JsonData.of(criteria.getToDate()));
                }
                return builder;
            })));
        }


        // Successful execution filter
        if (criteria.getSuccessful() != null) {
            boolBuilder.filter(q -> q.term(t -> t.field("successful")
                    .value(FieldValue.of(criteria.getSuccessful()))));
        }

        if (criteria.getMinTokenCount() != null || criteria.getMaxTokenCount() != null) {
            boolBuilder.filter(q -> q.range(r -> r.untyped(n -> {
                var builder = n.field("tokenCount");
                if (criteria.getMinTokenCount() != null) {
                    builder = builder.gte(JsonData.of(criteria.getMinTokenCount()));
                }
                if (criteria.getMaxTokenCount() != null) {
                    builder = builder.lte(JsonData.of(criteria.getMaxTokenCount()));
                }
                return builder;
            })));
        }



        return new SearchRequest.Builder()
                .index(indexName)
                .query(q -> q.bool(boolBuilder.build()))
                .from(pageable.getPageNumber() * pageable.getPageSize())
                .size(pageable.getPageSize())
                .sort(s -> s.field(f -> f.field("executedAt").order(SortOrder.Desc)))
                .build();
    }
}
