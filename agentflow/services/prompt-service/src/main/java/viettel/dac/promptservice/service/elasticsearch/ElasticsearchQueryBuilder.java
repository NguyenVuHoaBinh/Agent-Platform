package viettel.dac.promptservice.service.elasticsearch;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.json.JsonData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import viettel.dac.promptservice.dto.search.PromptTemplateSearchCriteria;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ElasticsearchQueryBuilder {

    /**
     * Build a search request for prompt templates.
     *
     * @param criteria Search criteria
     * @param pageable Pagination information
     * @param indexName Elasticsearch index name
     * @return Built search request
     */
    public SearchRequest buildTemplateSearchRequest(PromptTemplateSearchCriteria criteria, Pageable pageable, String indexName) {
        log.debug("Building Elasticsearch query for criteria: {}", criteria);

        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();

        // Text search on "name" and "description"
        if (StringUtils.hasText(criteria.getSearchText())) {
            if (Boolean.TRUE.equals(criteria.getUseExactMatch())) {
                // For exact matches, use phrase query
                boolBuilder.must(q -> q.multiMatch(m -> m
                        .fields("name^2", "description")
                        .query(criteria.getSearchText())
                        .type(TextQueryType.Phrase)));
            } else if (Boolean.TRUE.equals(criteria.getUseFuzzyMatch())) {
                // For fuzzy matches, allow some leeway
                boolBuilder.must(q -> q.multiMatch(m -> m
                        .fields("name^2", "description")
                        .query(criteria.getSearchText())
                        .fuzziness("AUTO")));
            } else {
                // Default search behavior
                boolBuilder.must(q -> q.multiMatch(m -> m
                        .fields("name^2", "description")
                        .query(criteria.getSearchText())));
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
            List<FieldValue> categoryValues = criteria.getCategories().stream()
                    .map(FieldValue::of)
                    .collect(Collectors.toList());

            boolBuilder.filter(q -> q.terms(t -> t.field("category")
                    .terms(terms -> terms.value(categoryValues))));
        }

        // Filter by creator
        if (StringUtils.hasText(criteria.getCreatedBy())) {
            boolBuilder.filter(q -> q.term(t -> t.field("createdBy")
                    .value(FieldValue.of(criteria.getCreatedBy()))));
        }

        // Date range filter for createdAt
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

        // Version count filter
        if (criteria.getMinVersionCount() != null) {
            boolBuilder.filter(q -> q.range(r -> r.untyped(n -> n.field("versionCount")
                    .gte(JsonData.of(criteria.getMinVersionCount())))));
        }


        // Build sort options
        List<co.elastic.clients.elasticsearch._types.SortOptions> sortOptions = new ArrayList<>();

        // Default sorting by relevance (if search text provided) or updated date
        if (StringUtils.hasText(criteria.getSearchText())) {
            // No explicit sort option needed - default score sorting applies
        } else {
            // Sort by updatedAt in descending order
            sortOptions.add(new co.elastic.clients.elasticsearch._types.SortOptions.Builder()
                    .field(f -> f.field("updatedAt").order(SortOrder.Desc))
                    .build());
        }

        // Build the final search request
        SearchRequest.Builder searchRequestBuilder = new SearchRequest.Builder()
                .index(indexName)
                .query(q -> q.bool(boolBuilder.build()))
                .from(pageable.getPageNumber() * pageable.getPageSize())
                .size(pageable.getPageSize());

        // Add sort options if any
        if (!sortOptions.isEmpty()) {
            searchRequestBuilder.sort(sortOptions);
        }

        // Add minimum score if specified
        if (criteria.getMinScore() != null) {
            searchRequestBuilder.minScore(criteria.getMinScore().doubleValue());
        }

        return searchRequestBuilder.build();
    }
}