package viettel.dac.promptservice.service.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import co.elastic.clients.elasticsearch.core.search.TotalHitsRelation;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import co.elastic.clients.elasticsearch.indices.ElasticsearchIndicesClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import viettel.dac.promptservice.dto.search.PromptTemplateSearchCriteria;
import viettel.dac.promptservice.model.document.PromptTemplateDocument;
import viettel.dac.promptservice.model.entity.PromptTemplate;
import viettel.dac.promptservice.model.entity.PromptVersion;
import viettel.dac.promptservice.model.enums.VersionStatus;
import viettel.dac.promptservice.repository.elasticsearch.PromptTemplateSearchRepository;
import viettel.dac.promptservice.repository.jpa.PromptTemplateRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class PromptSearchServiceTest {

    @Mock
    private PromptTemplateSearchRepository searchRepository;

    @Mock
    private DocumentConversionService conversionService;

    @Mock
    private ElasticsearchQueryBuilder queryBuilder;

    @Mock
    private ElasticsearchClient elasticsearchClient;

    @Mock
    private PromptTemplateRepository templateRepository;

    @InjectMocks
    private PromptSearchService searchService;

    // Test data
    private final String TEMPLATE_ID = "template-123";
    private final String INDEX_NAME = "prompt_templates";
    private PromptTemplate testTemplate;
    private PromptTemplateDocument testDocument;

    @BeforeEach
    void setUp() {
        // Set index name and batch size
        ReflectionTestUtils.setField(searchService, "promptTemplatesIndex", INDEX_NAME);
        ReflectionTestUtils.setField(searchService, "batchSize", 100);

        // Setup test template
        testTemplate = PromptTemplate.builder()
                .id(TEMPLATE_ID)
                .name("Test Template")
                .description("Test Description")
                .category("Test Category")
                .projectId("project-123")
                .createdBy("user-123")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Setup published version
        PromptVersion publishedVersion = PromptVersion.builder()
                .id("version-123")
                .template(testTemplate)
                .status(VersionStatus.PUBLISHED)
                .build();
        testTemplate.addVersion(publishedVersion);

        // Setup test document
        testDocument = PromptTemplateDocument.builder()
                .id(TEMPLATE_ID)
                .name("Test Template")
                .description("Test Description")
                .category("Test Category")
                .projectId("project-123")
                .createdBy("user-123")
                .build();
    }

    @Test
    @DisplayName("Should index a prompt template successfully")
    void shouldIndexPromptTemplateSuccessfully() {
        // Arrange
        when(conversionService.convertTemplateToDocument(testTemplate)).thenReturn(testDocument);

        // Act
        searchService.indexPromptTemplate(testTemplate);

        // Assert
        verify(conversionService).convertTemplateToDocument(testTemplate);
        verify(searchRepository).save(testDocument);
    }

    @Test
    @DisplayName("Should bulk index multiple templates successfully")
    void shouldBulkIndexTemplatesSuccessfully() {
        // Arrange
        PromptTemplate template2 = PromptTemplate.builder()
                .id("template-456")
                .name("Second Template")
                .build();

        List<PromptTemplate> templates = Arrays.asList(testTemplate, template2);

        PromptTemplateDocument document2 = PromptTemplateDocument.builder()
                .id("template-456")
                .name("Second Template")
                .build();

        // Stub individual conversion calls
        when(conversionService.convertTemplateToDocument(testTemplate)).thenReturn(testDocument);
        when(conversionService.convertTemplateToDocument(template2)).thenReturn(document2);

        // Act
        searchService.bulkIndexTemplates(templates);

        // Assert
        verify(conversionService).convertTemplateToDocument(testTemplate);
        verify(conversionService).convertTemplateToDocument(template2);
        // Assuming all documents are saved in one batch when batchSize is large enough.
        verify(searchRepository, times(1)).saveAll(argThat(iterable ->
                StreamSupport.stream(iterable.spliterator(), false).count() == 2
        ));

    }

    @Test
    @DisplayName("Should process batches during bulk indexing")
    void shouldProcessBatchesDuringBulkIndexing() {
        // Arrange
        ReflectionTestUtils.setField(searchService, "batchSize", 2);

        List<PromptTemplate> templates = new ArrayList<>();
        List<PromptTemplateDocument> documents = new ArrayList<>();

        // Create 5 templates and stub each conversion individually
        for (int i = 1; i <= 5; i++) {
            PromptTemplate template = PromptTemplate.builder()
                    .id("template-" + i)
                    .name("Template " + i)
                    .build();
            templates.add(template);

            PromptTemplateDocument document = PromptTemplateDocument.builder()
                    .id("template-" + i)
                    .name("Template " + i)
                    .build();
            documents.add(document);

            when(conversionService.convertTemplateToDocument(template)).thenReturn(document);
        }

        // Act
        searchService.bulkIndexTemplates(templates);

        // Assert
        // Each template conversion is invoked individually
        for (PromptTemplate template : templates) {
            verify(conversionService).convertTemplateToDocument(template);
        }
        // Should call saveAll three times with batches of size 2, 2, and 1
        verify(searchRepository, times(3)).saveAll(anyList());
    }

    @Test
    @DisplayName("Should handle empty list during bulk indexing")
    void shouldHandleEmptyListDuringBulkIndexing() {
        // Act
        searchService.bulkIndexTemplates(Collections.emptyList());

        // Assert
        verify(conversionService, never()).convertTemplateToDocument(any());
        verify(searchRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Should handle null list during bulk indexing")
    void shouldHandleNullListDuringBulkIndexing() {
        // Act
        searchService.bulkIndexTemplates(null);

        // Assert
        verify(conversionService, never()).convertTemplateToDocument(any());
        verify(searchRepository, never()).saveAll(anyList());
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("Should search templates by criteria")
    void shouldSearchTemplatesByCriteria() throws IOException {
        // Arrange
        PromptTemplateSearchCriteria criteria = PromptTemplateSearchCriteria.builder()
                .searchText("test")
                .projectId("project-123")
                .build();

        Pageable pageable = PageRequest.of(0, 10);

        // Create a concrete SearchRequest object
        SearchRequest searchRequest = new SearchRequest.Builder().build();

        // Set up mock search response
        SearchResponse<PromptTemplateDocument> searchResponse = mock(SearchResponse.class);
        HitsMetadata<PromptTemplateDocument> hitsMetadata = mock(HitsMetadata.class);
        List<Hit<PromptTemplateDocument>> hits = new ArrayList<>();

        Hit<PromptTemplateDocument> hit = mock(Hit.class);
        when(hit.id()).thenReturn(TEMPLATE_ID);
        hits.add(hit);

        when(hitsMetadata.hits()).thenReturn(hits);
        TotalHits totalHits = new TotalHits.Builder().value(1L).relation(TotalHitsRelation.valueOf("eq")).build();
        when(hitsMetadata.total()).thenReturn(totalHits);
        when(searchResponse.hits()).thenReturn(hitsMetadata);

        // Stub query builder to return a concrete SearchRequest
        when(queryBuilder.buildTemplateSearchRequest(eq(criteria), eq(pageable), eq(INDEX_NAME)))
                .thenReturn(searchRequest);

        // Stub elasticsearch client with specific method signature
        when(elasticsearchClient.search(any(SearchRequest.class), eq(PromptTemplateDocument.class)))
                .thenReturn(searchResponse);

        // Stub repository to return the template when finding by ID
        when(templateRepository.findAllById(Collections.singletonList(TEMPLATE_ID)))
                .thenReturn(Collections.singletonList(testTemplate));

        // Act
        Page<PromptTemplate> result = searchService.searchTemplates(criteria, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(TEMPLATE_ID, result.getContent().get(0).getId());

        // Verify interactions
        verify(queryBuilder).buildTemplateSearchRequest(eq(criteria), eq(pageable), eq(INDEX_NAME));
        verify(elasticsearchClient).search(any(SearchRequest.class), eq(PromptTemplateDocument.class));
        verify(templateRepository).findAllById(Collections.singletonList(TEMPLATE_ID));
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("Should handle empty search results")
    void shouldHandleEmptySearchResults() throws IOException {
        // Arrange
        PromptTemplateSearchCriteria criteria = PromptTemplateSearchCriteria.builder()
                .searchText("nonexistent")
                .build();

        Pageable pageable = PageRequest.of(0, 10);

        // Create a concrete SearchRequest object
        SearchRequest searchRequest = new SearchRequest.Builder().build();

        // Set up mock search response with no hits
        SearchResponse<PromptTemplateDocument> searchResponse = mock(SearchResponse.class);
        HitsMetadata<PromptTemplateDocument> hitsMetadata = mock(HitsMetadata.class);
        List<Hit<PromptTemplateDocument>> hits = Collections.emptyList();

        when(hitsMetadata.hits()).thenReturn(hits);
        TotalHits totalHits = new TotalHits.Builder().value(0L).relation(TotalHitsRelation.valueOf("eq")).build();
        when(hitsMetadata.total()).thenReturn(totalHits);
        when(searchResponse.hits()).thenReturn(hitsMetadata);

        // Stub query builder to return a concrete SearchRequest
        when(queryBuilder.buildTemplateSearchRequest(eq(criteria), eq(pageable), eq(INDEX_NAME)))
                .thenReturn(searchRequest);

        // Stub elasticsearch client with specific method signature
        when(elasticsearchClient.search(any(SearchRequest.class), eq(PromptTemplateDocument.class)))
                .thenReturn(searchResponse);

        // Act
        Page<PromptTemplate> result = searchService.searchTemplates(criteria, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertTrue(result.getContent().isEmpty());

        // Verify no database lookup was made
        verify(templateRepository, never()).findAllById(anyList());
    }

    @Test
    @DisplayName("Should handle elasticsearch exception during search")
    void shouldHandleElasticsearchExceptionDuringSearch() throws IOException {
        // Arrange
        PromptTemplateSearchCriteria criteria = PromptTemplateSearchCriteria.builder()
                .searchText("test")
                .build();

        Pageable pageable = PageRequest.of(0, 10);

        // Create a concrete SearchRequest object
        SearchRequest searchRequest = new SearchRequest.Builder().build();

        // Stub query builder to return a concrete SearchRequest
        when(queryBuilder.buildTemplateSearchRequest(eq(criteria), eq(pageable), eq(INDEX_NAME)))
                .thenReturn(searchRequest);

        // Stub elasticsearch client to throw exception
        when(elasticsearchClient.search(any(SearchRequest.class), eq(PromptTemplateDocument.class)))
                .thenThrow(new IOException("Elasticsearch error"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> searchService.searchTemplates(criteria, pageable));
    }

    @Test
    @DisplayName("Should delete template from index")
    void shouldDeleteTemplateFromIndex() {
        // Act
        searchService.deleteTemplateIndex(TEMPLATE_ID);

        // Assert
        verify(searchRepository).deleteById(TEMPLATE_ID);
    }

    @Test
    @DisplayName("Should check if index exists")
    void shouldCheckIfIndexExists() throws IOException {
        // Arrange
        // Create and stub a mock IndicesClient
        ElasticsearchIndicesClient indicesClient = mock(ElasticsearchIndicesClient.class);
        when(elasticsearchClient.indices()).thenReturn(indicesClient);

        BooleanResponse booleanResponse = mock(BooleanResponse.class);
        when(booleanResponse.value()).thenReturn(true);
        when(indicesClient.exists(any(ExistsRequest.class))).thenReturn(booleanResponse);

        // Act
        boolean result = searchService.isIndexAvailable();

        // Assert
        assertTrue(result);

        // Verify interactions
        verify(elasticsearchClient).indices();
        verify(indicesClient).exists(any(ExistsRequest.class));
    }

    @Test
    @DisplayName("Should handle exception when checking index")
    void shouldHandleExceptionWhenCheckingIndex() throws IOException {
        // Arrange
        // Create and stub a mock IndicesClient
        ElasticsearchIndicesClient indicesClient = mock(ElasticsearchIndicesClient.class);
        when(elasticsearchClient.indices()).thenReturn(indicesClient);

        when(indicesClient.exists(any(ExistsRequest.class)))
                .thenThrow(new IOException("Index check error"));

        // Act
        boolean result = searchService.isIndexAvailable();

        // Assert
        assertFalse(result);
    }

    @Test
    @DisplayName("Should retry on indexing failure")
    void shouldRetryOnIndexingFailure() {
        // Arrange
        when(conversionService.convertTemplateToDocument(testTemplate)).thenReturn(testDocument);

        // Simulate that each call to save(testDocument) throws an exception
        when(searchRepository.save(testDocument))
                .thenThrow(new RuntimeException("Indexing error"));

        // Act & Assert - should throw RuntimeException after retries are exhausted
        assertThrows(RuntimeException.class, () -> searchService.indexPromptTemplate(testTemplate));

        // Verify conversion service was called once
        verify(conversionService).convertTemplateToDocument(testTemplate);

        // Verify save was attempted (depending on retry configuration, adjust times if needed)
        verify(searchRepository, atLeast(1)).save(testDocument);
    }
}
