package viettel.dac.promptservice.config.elastic;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.util.StringUtils;

import javax.net.ssl.SSLContext;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Core Elasticsearch configuration for client and connection settings
 */
@Configuration
@EnableElasticsearchRepositories(basePackages = "viettel.dac.promptservice.repository.elasticsearch")
@RequiredArgsConstructor
@Slf4j
public class ElasticsearchConfig extends ElasticsearchConfiguration {

    private final ObjectMapper objectMapper;
    private final ElasticsearchSslConfig sslConfig;

    @Value("${spring.elasticsearch.uris}")
    private String[] elasticsearchUris;

    @Value("${spring.elasticsearch.username:}")
    private String elasticsearchUsername;

    @Value("${spring.elasticsearch.password:}")
    private String elasticsearchPassword;

    @Value("${spring.elasticsearch.socket-timeout:30000}")
    private int socketTimeout;

    @Value("${spring.elasticsearch.connection-timeout:5000}")
    private int connectionTimeout;

    @Value("${spring.elasticsearch.max-conn-total:40}")
    private int maxConnTotal;

    @Value("${spring.elasticsearch.max-conn-per-route:10}")
    private int maxConnPerRoute;

    @Value("${spring.elasticsearch.use-ssl:false}")
    private boolean useSsl;

    @Override
    public ClientConfiguration clientConfiguration() {
        ClientConfiguration.MaybeSecureClientConfigurationBuilder builder =
                (ClientConfiguration.MaybeSecureClientConfigurationBuilder) ClientConfiguration.builder()
                        .connectedTo(elasticsearchUris)
                        .withConnectTimeout(Duration.ofMillis(connectionTimeout))
                        .withSocketTimeout(Duration.ofMillis(socketTimeout));

        if (StringUtils.hasText(elasticsearchUsername) &&
                StringUtils.hasText(elasticsearchPassword)) {
            builder = (ClientConfiguration.MaybeSecureClientConfigurationBuilder) builder.withBasicAuth(elasticsearchUsername, elasticsearchPassword);
        }

        // Fixed SSL configuration
        if (useSsl) {
            try {
                SSLContext sslContext = sslConfig.buildSslContext();
                if (sslContext != null) {
                    return (ClientConfiguration) builder.usingSsl(sslContext);
                } else {
                    return (ClientConfiguration) builder.usingSsl();
                }
            } catch (Exception e) {
                log.error("Failed to configure SSL with certificate", e);
                return (ClientConfiguration) builder.usingSsl();
            }
        }

        log.info("Configured Elasticsearch connection to {}",
                Arrays.toString(elasticsearchUris));

        return builder.build();
    }

    @Bean
    public RestClient restClient() {
        List<HttpHost> httpHosts = Arrays.stream(elasticsearchUris)
                .map(uri -> {
                    String[] parts = uri.split(":");
                    String hostname = parts[0];
                    int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 9200;
                    String scheme = useSsl ? "https" : "http";
                    return new HttpHost(hostname, port, scheme);
                })
                .collect(Collectors.toList());

        RestClientBuilder builder = RestClient.builder(httpHosts.toArray(new HttpHost[0]))
                .setRequestConfigCallback(requestConfigBuilder ->
                        requestConfigBuilder
                                .setConnectTimeout(connectionTimeout)
                                .setSocketTimeout(socketTimeout))
                .setHttpClientConfigCallback(httpClientBuilder -> {
                    httpClientBuilder.setMaxConnTotal(maxConnTotal)
                            .setMaxConnPerRoute(maxConnPerRoute);

                    if (StringUtils.hasText(elasticsearchUsername) &&
                            StringUtils.hasText(elasticsearchPassword)) {
                        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                        credentialsProvider.setCredentials(AuthScope.ANY,
                                new UsernamePasswordCredentials(elasticsearchUsername, elasticsearchPassword));
                        httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                    }

                    if (useSsl) {
                        try {
                            SSLContext sslContext = sslConfig.buildSslContext();
                            if (sslContext != null) {
                                httpClientBuilder.setSSLContext(sslContext);
                            }
                        } catch (Exception e) {
                            log.error("Failed to configure SSL with certificate", e);
                        }
                    }

                    return httpClientBuilder;
                });

        return builder.build();
    }

    @Bean
    public ElasticsearchClient elasticsearchClient(RestClient restClient) {
        ObjectMapper esObjectMapper = objectMapper.copy();
        esObjectMapper.registerModule(new JavaTimeModule());

        JacksonJsonpMapper jsonpMapper = new JacksonJsonpMapper(esObjectMapper);
        ElasticsearchTransport transport = new RestClientTransport(restClient, jsonpMapper);

        return new ElasticsearchClient(transport);
    }

    @Bean
    public ElasticsearchOperations elasticsearchOperations(
            ElasticsearchClient elasticsearchClient) {
        ElasticsearchConverter converter = new MappingElasticsearchConverter(
                new SimpleElasticsearchMappingContext());

        return new ElasticsearchTemplate(elasticsearchClient, converter);
    }
}