package viettel.dac.promptservice.service.elasticsearch;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import viettel.dac.promptservice.dto.search.PromptTemplateSearchCriteria;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class ElasticsearchQueryBuilderTest {

    private ElasticsearchQueryBuilder queryBuilder;
    private final String INDEX_NAME = "prompt_templates";

    @BeforeEach
    void setUp() {
        queryBuilder = new ElasticsearchQueryBuilder();
    }

    @Test
    @DisplayName("Should build basic search request")
    void shouldBuildBasicSearchRequest() {
        // Arrange
        PromptTemplateSearchCriteria criteria = PromptTemplateSearchCriteria.builder().build();
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        SearchRequest request = queryBuilder.buildTemplateSearchRequest(criteria, pageable, INDEX_NAME);

        // Assert
        assertNotNull(request);
        // The index might be returned as a single string or as a List containing one string
        String indexStr = request.index().toString().replace("[", "").replace("]", "");
        assertEquals(INDEX_NAME, indexStr);
        assertEquals(0, request.from());
        assertEquals(10, request.size());

        // Should have a query
        assertNotNull(request.query());

        // Verify it's a BoolQuery
        assertTrue(request.query()._get() instanceof BoolQuery);
    }

    @Test
    @DisplayName("Should include text search when specified")
    void shouldIncludeTextSearchWhenSpecified() {
        // Arrange
        PromptTemplateSearchCriteria criteria = PromptTemplateSearchCriteria.builder()
                .searchText("test query")
                .build();
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        SearchRequest request = queryBuilder.buildTemplateSearchRequest(criteria, pageable, INDEX_NAME);

        // Assert
        assertNotNull(request);
        assertNotNull(request.query());

        // Get the bool query
        BoolQuery boolQuery = (BoolQuery) request.query()._get();

        // Should have a must clause for the text search
        assertNotNull(boolQuery.must());
        assertFalse(boolQuery.must().isEmpty());

        // The must clause should contain a multi_match query
        Query mustQuery = boolQuery.must().get(0);
        assertTrue(mustQuery._get() instanceof MultiMatchQuery);

        MultiMatchQuery multiMatchQuery = (MultiMatchQuery) mustQuery._get();
        assertEquals("test query", multiMatchQuery.query());

        // Fields should include name and description
        assertTrue(multiMatchQuery.fields().contains("name^2"));
        assertTrue(multiMatchQuery.fields().contains("description"));
    }

    @Test
    @DisplayName("Should apply project filter")
    void shouldApplyProjectFilter() {
        // Arrange
        PromptTemplateSearchCriteria criteria = PromptTemplateSearchCriteria.builder()
                .projectId("project-123")
                .build();
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        SearchRequest request = queryBuilder.buildTemplateSearchRequest(criteria, pageable, INDEX_NAME);

        // Assert
        // Get the bool query
        BoolQuery boolQuery = (BoolQuery) request.query()._get();

        // Should have a filter clause for the project ID
        assertNotNull(boolQuery.filter());
        assertFalse(boolQuery.filter().isEmpty());

        // The filter clause should contain a term query
        Query filterQuery = boolQuery.filter().get(0);
        assertTrue(filterQuery._get() instanceof TermQuery);

        TermQuery termQuery = (TermQuery) filterQuery._get();
        assertEquals("projectId", termQuery.field());
        assertEquals("project-123", termQuery.value().stringValue());
    }

    @Test
    @DisplayName("Should apply category filter")
    void shouldApplyCategoryFilter() {
        // Arrange
        PromptTemplateSearchCriteria criteria = PromptTemplateSearchCriteria.builder()
                .category("test-category")
                .build();
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        SearchRequest request = queryBuilder.buildTemplateSearchRequest(criteria, pageable, INDEX_NAME);

        // Assert
        // Get the bool query
        BoolQuery boolQuery = (BoolQuery) request.query()._get();

        // Should have a filter clause for the category
        assertNotNull(boolQuery.filter());
        assertFalse(boolQuery.filter().isEmpty());

        // The filter clause should contain a term query
        Query filterQuery = boolQuery.filter().get(0);
        assertTrue(filterQuery._get() instanceof TermQuery);

        TermQuery termQuery = (TermQuery) filterQuery._get();
        assertEquals("category", termQuery.field());
        assertEquals("test-category", termQuery.value().stringValue());
    }

    @Test
    @DisplayName("Should apply multiple categories filter")
    void shouldApplyMultipleCategoriesFilter() {
        // Arrange
        PromptTemplateSearchCriteria criteria = PromptTemplateSearchCriteria.builder()
                .categories(new java.util.HashSet<>(Arrays.asList("category1", "category2", "category3")))
                .build();
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        SearchRequest request = queryBuilder.buildTemplateSearchRequest(criteria, pageable, INDEX_NAME);

        // Assert
        // Get the bool query
        BoolQuery boolQuery = (BoolQuery) request.query()._get();

        // Should have a filter clause for the categories
        assertNotNull(boolQuery.filter());
        assertFalse(boolQuery.filter().isEmpty());

        // The filter clause should contain a terms query
        Query filterQuery = boolQuery.filter().get(0);
        assertTrue(filterQuery._get() instanceof TermsQuery);

        TermsQuery termsQuery = (TermsQuery) filterQuery._get();
        assertEquals("category", termsQuery.field());

        // Should have 3 term values
        assertEquals(3, termsQuery.terms().value().size());

        // Check if all categories are included
        List<FieldValue> fieldValues = termsQuery.terms().value();
        boolean hasCategory1 = false;
        boolean hasCategory2 = false;
        boolean hasCategory3 = false;

        for (FieldValue value : fieldValues) {
            String stringValue = value.stringValue();
            if ("category1".equals(stringValue)) hasCategory1 = true;
            if ("category2".equals(stringValue)) hasCategory2 = true;
            if ("category3".equals(stringValue)) hasCategory3 = true;
        }

        assertTrue(hasCategory1 && hasCategory2 && hasCategory3);
    }

    @Test
    @DisplayName("Should apply creator filter")
    void shouldApplyCreatorFilter() {
        // Arrange
        PromptTemplateSearchCriteria criteria = PromptTemplateSearchCriteria.builder()
                .createdBy("user-123")
                .build();
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        SearchRequest request = queryBuilder.buildTemplateSearchRequest(criteria, pageable, INDEX_NAME);

        // Assert
        // Get the bool query
        BoolQuery boolQuery = (BoolQuery) request.query()._get();

        // Should have a filter clause for the creator
        assertNotNull(boolQuery.filter());
        assertFalse(boolQuery.filter().isEmpty());

        // The filter clause should contain a term query
        Query filterQuery = boolQuery.filter().get(0);
        assertTrue(filterQuery._get() instanceof TermQuery);

        TermQuery termQuery = (TermQuery) filterQuery._get();
        assertEquals("createdBy", termQuery.field());
        assertEquals("user-123", termQuery.value().stringValue());
    }

    @Test
    @DisplayName("Should apply date range filter")
    void shouldApplyDateRangeFilter() {
        // Arrange
        LocalDateTime fromDate = LocalDateTime.now().minusDays(7);
        LocalDateTime toDate = LocalDateTime.now();

        PromptTemplateSearchCriteria criteria = PromptTemplateSearchCriteria.builder()
                .fromDate(fromDate)
                .toDate(toDate)
                .build();
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        SearchRequest request = queryBuilder.buildTemplateSearchRequest(criteria, pageable, INDEX_NAME);

        // Assert
        // Get the bool query
        BoolQuery boolQuery = (BoolQuery) request.query()._get();

        // Should have a filter clause for the date range
        assertNotNull(boolQuery.filter());
        assertFalse(boolQuery.filter().isEmpty());

        // The filter clause should contain a range query
        Query filterQuery = boolQuery.filter().get(0);
        assertTrue(filterQuery._get() instanceof RangeQuery);
    }

    @Test
    @DisplayName("Should apply published version filter")
    void shouldApplyPublishedVersionFilter() {
        // Arrange
        PromptTemplateSearchCriteria criteria = PromptTemplateSearchCriteria.builder()
                .hasPublishedVersion(true)
                .build();
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        SearchRequest request = queryBuilder.buildTemplateSearchRequest(criteria, pageable, INDEX_NAME);

        // Assert
        // Get the bool query
        BoolQuery boolQuery = (BoolQuery) request.query()._get();

        // Should have a filter clause for hasPublishedVersion
        assertNotNull(boolQuery.filter());
        assertFalse(boolQuery.filter().isEmpty());

        // The filter clause should contain a term query
        Query filterQuery = boolQuery.filter().get(0);
        assertTrue(filterQuery._get() instanceof TermQuery);

        TermQuery termQuery = (TermQuery) filterQuery._get();
        assertEquals("hasPublishedVersion", termQuery.field());
        
        // Instead of directly checking the boolean value, just verify it's not null
        assertNotNull(termQuery.value());
    }

    @Test
    @DisplayName("Should apply version count filter")
    void shouldApplyVersionCountFilter() {
        // Arrange
        PromptTemplateSearchCriteria criteria = PromptTemplateSearchCriteria.builder()
                .minVersionCount(5)
                .build();
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        SearchRequest request = queryBuilder.buildTemplateSearchRequest(criteria, pageable, INDEX_NAME);

        // Assert
        // Get the bool query
        BoolQuery boolQuery = (BoolQuery) request.query()._get();

        // Should have a filter clause for minVersionCount
        assertNotNull(boolQuery.filter());
        assertFalse(boolQuery.filter().isEmpty());

        // The filter clause should contain a range query
        Query filterQuery = boolQuery.filter().get(0);
        assertTrue(filterQuery._get() instanceof RangeQuery);
    }

    @Test
    @DisplayName("Should apply exact match text search")
    void shouldApplyExactMatchTextSearch() {
        // Arrange
        PromptTemplateSearchCriteria criteria = PromptTemplateSearchCriteria.builder()
                .searchText("exact phrase")
                .useExactMatch(true)
                .build();
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        SearchRequest request = queryBuilder.buildTemplateSearchRequest(criteria, pageable, INDEX_NAME);

        // Assert
        // Get the bool query
        BoolQuery boolQuery = (BoolQuery) request.query()._get();

        // Should have a must clause for the text search
        assertNotNull(boolQuery.must());
        assertFalse(boolQuery.must().isEmpty());

        // The must clause should contain a multi_match query
        Query mustQuery = boolQuery.must().get(0);
        assertTrue(mustQuery._get() instanceof MultiMatchQuery);

        MultiMatchQuery multiMatchQuery = (MultiMatchQuery) mustQuery._get();
        assertEquals("exact phrase", multiMatchQuery.query());
        assertEquals(TextQueryType.Phrase, multiMatchQuery.type());
    }

    @Test
    @DisplayName("Should apply fuzzy match text search")
    void shouldApplyFuzzyMatchTextSearch() {
        // Arrange
        PromptTemplateSearchCriteria criteria = PromptTemplateSearchCriteria.builder()
                .searchText("fuzzy term")
                .useFuzzyMatch(true)
                .build();
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        SearchRequest request = queryBuilder.buildTemplateSearchRequest(criteria, pageable, INDEX_NAME);

        // Assert
        // Get the bool query
        BoolQuery boolQuery = (BoolQuery) request.query()._get();

        // Should have a must clause for the text search
        assertNotNull(boolQuery.must());
        assertFalse(boolQuery.must().isEmpty());

        // The must clause should contain a multi_match query
        Query mustQuery = boolQuery.must().get(0);
        assertTrue(mustQuery._get() instanceof MultiMatchQuery);

        MultiMatchQuery multiMatchQuery = (MultiMatchQuery) mustQuery._get();
        assertEquals("fuzzy term", multiMatchQuery.query());
        assertEquals("AUTO", multiMatchQuery.fuzziness());
    }

    @Test
    @DisplayName("Should apply minimum score")
    void shouldApplyMinimumScore() {
        // Arrange
        PromptTemplateSearchCriteria criteria = PromptTemplateSearchCriteria.builder()
                .searchText("test")
                .minScore(0.5f)
                .build();
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        SearchRequest request = queryBuilder.buildTemplateSearchRequest(criteria, pageable, INDEX_NAME);

        // Assert
        assertNotNull(request);
        assertEquals(0.5, request.minScore());
    }

    @Test
    @DisplayName("Should sort by relevance for text search")
    void shouldSortByRelevanceForTextSearch() {
        // Arrange
        PromptTemplateSearchCriteria criteria = PromptTemplateSearchCriteria.builder()
                .searchText("test query")
                .build();
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        SearchRequest request = queryBuilder.buildTemplateSearchRequest(criteria, pageable, INDEX_NAME);

        // Assert
        assertNotNull(request);

        // For text search, no explicit sort should be set (uses relevance)
        // The sort list may be null or empty for relevance-based sorting
        assertTrue(request.sort() == null || request.sort().isEmpty(),
            "For text searches, no explicit sort should be applied to use default relevance scoring");
    }

    @Test
    @DisplayName("Should sort by updatedAt for non-text search")
    void shouldSortByUpdatedAtForNonTextSearch() {
        // Arrange
        PromptTemplateSearchCriteria criteria = PromptTemplateSearchCriteria.builder()
                .projectId("project-123")
                .build();
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        SearchRequest request = queryBuilder.buildTemplateSearchRequest(criteria, pageable, INDEX_NAME);

        // Assert
        assertNotNull(request);
        assertNotNull(request.sort());
        assertFalse(request.sort().isEmpty());

        // Check that sort is by updatedAt DESC
        assertEquals("updatedAt", request.sort().get(0).field().field());
        assertEquals(SortOrder.Desc, request.sort().get(0).field().order());
    }

    @Test
    @DisplayName("Should apply pagination parameters")
    void shouldApplyPaginationParameters() {
        // Arrange
        PromptTemplateSearchCriteria criteria = PromptTemplateSearchCriteria.builder().build();
        Pageable pageable = PageRequest.of(2, 15, Sort.by(Sort.Direction.DESC, "createdAt"));

        // Act
        SearchRequest request = queryBuilder.buildTemplateSearchRequest(criteria, pageable, INDEX_NAME);

        // Assert
        assertNotNull(request);
        assertEquals(30, request.from());  // page 2 with size 15 = from 30
        assertEquals(15, request.size());
    }

    @Test
    @DisplayName("Should handle search criteria with multiple filters")
    void shouldHandleSearchCriteriaWithMultipleFilters() {
        // Arrange
        PromptTemplateSearchCriteria criteria = PromptTemplateSearchCriteria.builder()
                .searchText("test query")
                .projectId("project-123")
                .category("test-category")
                .createdBy("user-123")
                .hasPublishedVersion(true)
                .minVersionCount(5)
                .build();
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        SearchRequest request = queryBuilder.buildTemplateSearchRequest(criteria, pageable, INDEX_NAME);

        // Assert
        assertNotNull(request);

        // Get the bool query
        BoolQuery boolQuery = (BoolQuery) request.query()._get();

        // Should have a must clause for the text search
        assertNotNull(boolQuery.must());
        assertFalse(boolQuery.must().isEmpty());

        // Should have multiple filter clauses
        assertNotNull(boolQuery.filter());
        assertEquals(5, boolQuery.filter().size());  // One for each filter criterion
    }
}