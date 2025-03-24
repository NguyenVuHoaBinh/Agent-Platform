package viettel.dac.promptservice.config.elastic;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import lombok.Getter;

/**
 * Configuration for Elasticsearch index settings
 */
@Configuration
@Getter
public class ElasticsearchIndexConfig {

    @Value("${spring.elasticsearch.index.prompt-templates:prompt_templates}")
    private String promptTemplatesIndex;

    @Value("${spring.elasticsearch.index.prompt-versions:prompt_versions}")
    private String promptVersionsIndex;

    @Value("${spring.elasticsearch.index.prompt-executions:prompt_executions}")
    private String promptExecutionsIndex;

    @Value("${spring.elasticsearch.index.number-of-shards:5}")
    private int numberOfShards;

    @Value("${spring.elasticsearch.index.number-of-replicas:1}")
    private int numberOfReplicas;

    @Bean
    public ElasticsearchIndexManager indexManager(
            ElasticsearchTemplate elasticsearchTemplate,
            ElasticsearchClient client) {
        return new ElasticsearchIndexManager(
                elasticsearchTemplate, client,
                promptTemplatesIndex, promptVersionsIndex, promptExecutionsIndex,
                numberOfShards, numberOfReplicas);
    }
}