package viettel.dac.promptservice.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

/**
 * Mock configuration for Elasticsearch in tests
 */
@Configuration
@Profile("test")
@ConditionalOnProperty(name = "spring.elasticsearch.enabled", havingValue = "false")
public class TestElasticsearchConfig {

    @Bean
    @Primary
    public ElasticsearchClient elasticsearchClient() {
        return Mockito.mock(ElasticsearchClient.class);
    }

    @Bean
    @Primary
    public ElasticsearchOperations elasticsearchOperations() {
        return Mockito.mock(ElasticsearchOperations.class);
    }

    @Bean
    @Primary
    public ElasticsearchTemplate elasticsearchTemplate() {
        return Mockito.mock(ElasticsearchTemplate.class);
    }
} 