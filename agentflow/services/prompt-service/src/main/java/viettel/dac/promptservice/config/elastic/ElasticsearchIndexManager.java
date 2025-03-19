package viettel.dac.promptservice.config.elastic;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

import java.io.IOException;

/**
 * Manager for Elasticsearch index operations
 */
@RequiredArgsConstructor
@Slf4j
public class ElasticsearchIndexManager {
    private final ElasticsearchOperations operations;
    private final ElasticsearchClient client;
    private final String promptTemplatesIndex;
    private final String promptVersionsIndex;
    private final String promptExecutionsIndex;
    private final int numberOfShards;
    private final int numberOfReplicas;

    /**
     * Create or update all required indices
     */
    public void createIndicesIfNotExist() throws IOException {
        createPromptTemplatesIndex();
        createPromptVersionsIndex();
        createPromptExecutionsIndex();
    }

    private void createPromptTemplatesIndex() throws IOException {
        if (!indexExists(promptTemplatesIndex)) {
            client.indices().create(c -> c
                    .index(promptTemplatesIndex)
                    .settings(s -> s
                            .numberOfShards(String.valueOf(numberOfShards))
                            .numberOfReplicas(String.valueOf(numberOfReplicas))
                            .analysis(a -> a
                                            .analyzer("text_analyzer", analyzer -> analyzer
                                                    .custom(custom -> custom
                                                            .tokenizer("standard")
                                                            .filter("lowercase")  // Removed "stop" and "snowball" filters
                                                    )
                                            )
                                    // Removed snowball filter configuration
                            )
                    )
                    .mappings(m -> m
                            .properties("name", p -> p.text(t -> t
                                    .analyzer("text_analyzer")
                                    .fields("keyword", f -> f.keyword(k -> k))
                            ))
                            .properties("description", p -> p.text(t -> t
                                    .analyzer("text_analyzer")
                            ))
                            .properties("category", p -> p.keyword(k -> k))
                            .properties("createdAt", p -> p.date(d -> d))
                            .properties("updatedAt", p -> p.date(d -> d))
                            .properties("projectId", p -> p.keyword(k -> k))
                            .properties("createdBy", p -> p.keyword(k -> k))
                    )
            );
        }
    }


    /**
     * Create the prompt versions index with appropriate mappings
     */
    private void createPromptVersionsIndex() throws IOException {
        if (!indexExists(promptVersionsIndex)) {
            client.indices().create(c -> c
                    .index(promptVersionsIndex)
                    .settings(s -> s
                            .numberOfShards(String.valueOf(numberOfShards))
                            .numberOfReplicas(String.valueOf(numberOfReplicas))
                            .analysis(a -> a
                                    .analyzer("prompt_analyzer", analyzer -> analyzer
                                            .custom(custom -> custom
                                                    .tokenizer("standard")
                                                    .filter("lowercase", "stop")
                                            )
                                    )
                            )
                    )
                    .mappings(m -> m
                            .properties("templateId", p -> p.keyword(k -> k))
                            .properties("versionNumber", p -> p.keyword(k -> k))
                            .properties("content", p -> p.text(t -> t
                                    .analyzer("prompt_analyzer")
                            ))
                            .properties("status", p -> p.keyword(k -> k))
                            .properties("createdAt", p -> p.date(d -> d))
                            .properties("createdBy", p -> p.keyword(k -> k))
                            .properties("parameters", p -> p.nested(n -> n
                                    .properties("name", np -> np.keyword(k -> k))
                                    .properties("description", np -> np.text(t -> t))
                                    .properties("parameterType", np -> np.keyword(k -> k))
                                    .properties("required", np -> np.boolean_(b -> b))
                            ))
                    )
            );
        }
    }

    /**
     * Create the prompt executions index with appropriate mappings
     */
    private void createPromptExecutionsIndex() throws IOException {
        if (!indexExists(promptExecutionsIndex)) {
            client.indices().create(c -> c
                    .index(promptExecutionsIndex)
                    .settings(s -> s
                            .numberOfShards(String.valueOf(numberOfShards))
                            .numberOfReplicas(String.valueOf(numberOfReplicas))
                    )
                    .mappings(m -> m
                            .properties("versionId", p -> p.keyword(k -> k))
                            .properties("providerId", p -> p.keyword(k -> k))
                            .properties("modelId", p -> p.keyword(k -> k))
                            .properties("inputParameters", p -> p.object(o -> o))
                            .properties("rawResponse", p -> p.text(t -> t
                                    .index(false)
                            ))
                            .properties("tokenCount", p -> p.integer(i -> i))
                            .properties("inputTokens", p -> p.integer(i -> i))
                            .properties("outputTokens", p -> p.integer(i -> i))
                            .properties("cost", p -> p.float_(f -> f))
                            .properties("responseTimeMs", p -> p.long_(l -> l))
                            .properties("executedAt", p -> p.date(d -> d))
                            .properties("executedBy", p -> p.keyword(k -> k))
                            .properties("status", p -> p.keyword(k -> k))
                    )
            );
        }
    }

    /**
     * Check if an index exists
     */
    private boolean indexExists(String indexName) throws IOException {
        return client.indices().exists(e -> e.index(indexName)).value();
    }

    /**
     * Delete and recreate all indices (for development/testing)
     */
    public void recreateIndices() throws IOException {
        deleteIndices();
        createIndicesIfNotExist();
    }

    /**
     * Delete all indices
     */
    private void deleteIndices() throws IOException {
        if (indexExists(promptTemplatesIndex)) {
            client.indices().delete(d -> d.index(promptTemplatesIndex));
        }
        if (indexExists(promptVersionsIndex)) {
            client.indices().delete(d -> d.index(promptVersionsIndex));
        }
        if (indexExists(promptExecutionsIndex)) {
            client.indices().delete(d -> d.index(promptExecutionsIndex));
        }
    }
}
