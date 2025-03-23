package viettel.dac.promptservice.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchRepositoriesAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configuration to disable auto-configuration of Elasticsearch repositories in tests
 */
@Configuration
@Profile("test")
@ConditionalOnProperty(name = "spring.elasticsearch.enabled", havingValue = "false", matchIfMissing = false)
@EnableAutoConfiguration(exclude = {ElasticsearchRepositoriesAutoConfiguration.class})
public class ElasticsearchRepositoriesConfig {
    // Simply here to disable Elasticsearch repositories
} 