package viettel.dac.promptservice.config.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import co.elastic.clients.elasticsearch.indices.DeleteIndexResponse;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import co.elastic.clients.util.ObjectBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import viettel.dac.promptservice.config.elastic.ElasticsearchIndexManager;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ElasticsearchIndexManagerTest {

    @Mock
    private ElasticsearchOperations operations;

    @Mock
    private ElasticsearchClient client;

    @Mock
    private co.elastic.clients.elasticsearch.indices.ElasticsearchIndicesClient indicesClient;

    private ElasticsearchIndexManager indexManager;

    private final String promptTemplatesIndex = "prompt_templates";
    private final String promptVersionsIndex = "prompt_versions";
    private final String promptExecutionsIndex = "prompt_executions";
    private final int numberOfShards = 2;
    private final int numberOfReplicas = 1;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        when(client.indices()).thenReturn(indicesClient);

        indexManager = new ElasticsearchIndexManager(
                operations,
                client,
                promptTemplatesIndex,
                promptVersionsIndex,
                promptExecutionsIndex,
                numberOfShards,
                numberOfReplicas
        );
    }

    @Test
    public void testIndexExists_True() throws Exception {
        // Arrange
        BooleanResponse existsResponse = new BooleanResponse(true);
        when(indicesClient.exists((Function<ExistsRequest.Builder, ObjectBuilder<ExistsRequest>>) any()))
                .thenReturn(existsResponse);

        // Act - use reflection to access private method
        Method indexExistsMethod = ElasticsearchIndexManager.class.getDeclaredMethod("indexExists", String.class);
        indexExistsMethod.setAccessible(true);
        boolean result = (boolean) indexExistsMethod.invoke(indexManager, promptTemplatesIndex);

        // Assert
        assertTrue(result);
        verify(indicesClient).exists((Function<ExistsRequest.Builder, ObjectBuilder<ExistsRequest>>) any());
    }

    @Test
    public void testIndexExists_False() throws Exception {
        // Arrange
        BooleanResponse existsResponse = new BooleanResponse(false);
        when(indicesClient.exists((Function<ExistsRequest.Builder, ObjectBuilder<ExistsRequest>>) any()))
                .thenReturn(existsResponse);

        // Act - use reflection to access private method
        Method indexExistsMethod = ElasticsearchIndexManager.class.getDeclaredMethod("indexExists", String.class);
        indexExistsMethod.setAccessible(true);
        boolean result = (boolean) indexExistsMethod.invoke(indexManager, promptTemplatesIndex);

        // Assert
        assertFalse(result);
        verify(indicesClient).exists((Function<ExistsRequest.Builder, ObjectBuilder<ExistsRequest>>) any());
    }

    @Test
    public void testCreateIndicesIfNotExist_AllIndicesExist() throws IOException {
        // Arrange
        BooleanResponse existsResponse = new BooleanResponse(true);
        when(indicesClient.exists((Function<ExistsRequest.Builder, ObjectBuilder<ExistsRequest>>) any()))
                .thenReturn(existsResponse);

        // Act
        indexManager.createIndicesIfNotExist();

        // Assert - verify client called for existence check but not for creation
        verify(indicesClient, times(3)).exists((Function<ExistsRequest.Builder, ObjectBuilder<ExistsRequest>>) any());
        verify(indicesClient, never()).create(any(CreateIndexRequest.class));
    }

    @Test
    public void testCreateIndicesIfNotExist_NoIndicesExist() throws IOException {
        // Arrange
        BooleanResponse existsResponse = new BooleanResponse(false);
        CreateIndexResponse createResponse = mock(CreateIndexResponse.class);

        when(indicesClient.exists((Function<ExistsRequest.Builder, ObjectBuilder<ExistsRequest>>) any()))
                .thenReturn(existsResponse);
        when(indicesClient.create(any(CreateIndexRequest.class))).thenReturn(createResponse);
        when(indicesClient.create(any(CreateIndexRequest.class))).thenReturn(createResponse);

        // Act
        indexManager.createIndicesIfNotExist();

        // Assert - verify client called for existence check and creation
        verify(indicesClient, times(3)).exists((Function<ExistsRequest.Builder, ObjectBuilder<ExistsRequest>>) any());
        verify(indicesClient, times(3)).create(any(CreateIndexRequest.class));
    }

    @Test
    public void testCreatePromptTemplatesIndex_IndexDoesNotExist() throws Exception {
        // Arrange
        BooleanResponse existsResponse = new BooleanResponse(false);
        CreateIndexResponse createResponse = mock(CreateIndexResponse.class);

        when(indicesClient.exists((Function<ExistsRequest.Builder, ObjectBuilder<ExistsRequest>>) any()))
                .thenReturn(existsResponse);
        when(indicesClient.create(any(CreateIndexRequest.class))).thenReturn(createResponse);

        // Act - call private method via reflection
        Method method = ElasticsearchIndexManager.class.getDeclaredMethod("createPromptTemplatesIndex");
        method.setAccessible(true);
        method.invoke(indexManager);

        // Assert
        verify(indicesClient).exists((Function<ExistsRequest.Builder, ObjectBuilder<ExistsRequest>>) any());
        verify(indicesClient).create(any(CreateIndexRequest.class));
    }

    @Test
    public void testCreatePromptTemplatesIndex_IndexExists() throws Exception {
        // Arrange
        BooleanResponse existsResponse = new BooleanResponse(true);
        when(indicesClient.exists((Function<ExistsRequest.Builder, ObjectBuilder<ExistsRequest>>) any()))
                .thenReturn(existsResponse);

        // Act - call private method via reflection
        Method method = ElasticsearchIndexManager.class.getDeclaredMethod("createPromptTemplatesIndex");
        method.setAccessible(true);
        method.invoke(indexManager);

        // Assert
        verify(indicesClient).exists((Function<ExistsRequest.Builder, ObjectBuilder<ExistsRequest>>) any());
        verify(indicesClient, never()).create(any(CreateIndexRequest.class));
    }

    @Test
    public void testCreatePromptVersionsIndex_IndexDoesNotExist() throws Exception {
        // Arrange
        BooleanResponse existsResponse = new BooleanResponse(false);
        CreateIndexResponse createResponse = mock(CreateIndexResponse.class);

        when(indicesClient.exists((Function<ExistsRequest.Builder, ObjectBuilder<ExistsRequest>>) any()))
                .thenReturn(existsResponse);
        when(indicesClient.create(any(CreateIndexRequest.class))).thenReturn(createResponse);

        // Act - call private method via reflection
        Method method = ElasticsearchIndexManager.class.getDeclaredMethod("createPromptVersionsIndex");
        method.setAccessible(true);
        method.invoke(indexManager);

        // Assert
        verify(indicesClient).exists((Function<ExistsRequest.Builder, ObjectBuilder<ExistsRequest>>) any());
        verify(indicesClient).create(any(CreateIndexRequest.class));
    }

    @Test
    public void testCreatePromptExecutionsIndex_IndexDoesNotExist() throws Exception {
        // Arrange
        BooleanResponse existsResponse = new BooleanResponse(false);
        CreateIndexResponse createResponse = mock(CreateIndexResponse.class);

        when(indicesClient.exists((Function<ExistsRequest.Builder, ObjectBuilder<ExistsRequest>>) any()))
                .thenReturn(existsResponse);
        when(indicesClient.create(any(CreateIndexRequest.class))).thenReturn(createResponse);

        // Act - call private method via reflection
        Method method = ElasticsearchIndexManager.class.getDeclaredMethod("createPromptExecutionsIndex");
        method.setAccessible(true);
        method.invoke(indexManager);

        // Assert
        verify(indicesClient).exists((Function<ExistsRequest.Builder, ObjectBuilder<ExistsRequest>>) any());
        verify(indicesClient).create(any(CreateIndexRequest.class));
    }

    @Test
    public void testDeleteIndices_AllIndicesExist() throws Exception {
        // Arrange
        BooleanResponse existsResponse = new BooleanResponse(true);
        DeleteIndexResponse deleteResponse = mock(DeleteIndexResponse.class);

        when(indicesClient.exists((Function<ExistsRequest.Builder, ObjectBuilder<ExistsRequest>>) any()))
                .thenReturn(existsResponse);
        when(indicesClient.delete(any(DeleteIndexRequest.class))).thenReturn(deleteResponse);

        // Act - call private method via reflection
        Method method = ElasticsearchIndexManager.class.getDeclaredMethod("deleteIndices");
        method.setAccessible(true);
        method.invoke(indexManager);

        // Assert
        verify(indicesClient, times(3)).exists((Function<ExistsRequest.Builder, ObjectBuilder<ExistsRequest>>) any());
        verify(indicesClient, times(3)).delete(any(DeleteIndexRequest.class));
    }

    @Test
    public void testDeleteIndices_NoIndicesExist() throws Exception {
        // Arrange
        BooleanResponse existsResponse = new BooleanResponse(false);
        when(indicesClient.exists((Function<ExistsRequest.Builder, ObjectBuilder<ExistsRequest>>) any()))
                .thenReturn(existsResponse);

        // Act - call private method via reflection
        Method method = ElasticsearchIndexManager.class.getDeclaredMethod("deleteIndices");
        method.setAccessible(true);
        method.invoke(indexManager);

        // Assert
        verify(indicesClient, times(3)).exists((Function<ExistsRequest.Builder, ObjectBuilder<ExistsRequest>>) any());
        verify(indicesClient, never()).delete(any(DeleteIndexRequest.class));
    }

    @Test
    public void testRecreateIndices() throws IOException {
        // Arrange
        BooleanResponse existsResponse = new BooleanResponse(true);
        DeleteIndexResponse deleteResponse = mock(DeleteIndexResponse.class);
        CreateIndexResponse createResponse = mock(CreateIndexResponse.class);

        when(indicesClient.exists((Function<ExistsRequest.Builder, ObjectBuilder<ExistsRequest>>) any()))
                .thenReturn(existsResponse);
        when(indicesClient.delete(any(DeleteIndexRequest.class))).thenReturn(deleteResponse);
        when(indicesClient.create(any(CreateIndexRequest.class))).thenReturn(createResponse);

        // Act
        indexManager.recreateIndices();

        // Assert - verify delete and create called for each index
        verify(indicesClient, times(6)).exists((Function<ExistsRequest.Builder, ObjectBuilder<ExistsRequest>>) any()); // 3 for delete checks, 3 for create checks
        verify(indicesClient, times(3)).delete(any(DeleteIndexRequest.class));
        verify(indicesClient, times(3)).create(any(CreateIndexRequest.class));
    }

    @Test
    public void testCreateIndicesIfNotExist_Exception() throws IOException {
        // Arrange
        when(indicesClient.exists((Function<ExistsRequest.Builder, ObjectBuilder<ExistsRequest>>) any()))
                .thenThrow(new IOException("Connection error"));

        // Act & Assert
        assertThrows(IOException.class, () -> indexManager.createIndicesIfNotExist());
    }
}
