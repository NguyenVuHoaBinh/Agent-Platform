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
import org.springframework.context.annotation.Primary;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
import org.springframework.data.elasticsearch.core.convert.ElasticsearchCustomConversions;
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter;
import org.springframework.data.elasticsearch.core.mapping.SimpleElasticsearchMappingContext;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.core.convert.converter.Converter;
import org.springframework.util.StringUtils;

import javax.net.ssl.SSLContext;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Core Elasticsearch configuration for client and connection settings
 */
@Configuration
@EnableElasticsearchRepositories(basePackages = "viettel.dac.promptservice.repository.elasticsearch")
@RequiredArgsConstructor
@Slf4j
public class ElasticsearchConfig {

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

    @Bean
    @Primary
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

        log.info("Configured Elasticsearch connection to {}",
                Arrays.toString(elasticsearchUris));

        return builder.build();
    }

    @Bean
    public ElasticsearchTransport elasticsearchTransport(RestClient restClient) {
        ObjectMapper esObjectMapper = objectMapper.copy();
        esObjectMapper.registerModule(new JavaTimeModule());
        JacksonJsonpMapper jsonpMapper = new JacksonJsonpMapper(esObjectMapper);
        return new RestClientTransport(restClient, jsonpMapper);
    }

    @Bean
    public ElasticsearchClient elasticsearchClient(ElasticsearchTransport transport) {
        return new ElasticsearchClient(transport);
    }

    @Bean
    public ElasticsearchCustomConversions elasticsearchCustomConversions() {
        return new ElasticsearchCustomConversions(
            Collections.singletonList(new BigDecimalToDoubleConverter())
        );
    }

    @Bean
    public ElasticsearchConverter elasticsearchConverter() {
        MappingElasticsearchConverter converter = new MappingElasticsearchConverter(new SimpleElasticsearchMappingContext());
        converter.setConversions(elasticsearchCustomConversions());
        return converter;
    }

    @Bean
    @Primary
    public ElasticsearchTemplate elasticsearchTemplate(ElasticsearchClient elasticsearchClient) {
        return new ElasticsearchTemplate(elasticsearchClient, elasticsearchConverter());
    }

    /**
     * Custom converter for BigDecimal to avoid reflection issues with JDK modules
     */
    static class BigDecimalToDoubleConverter implements Converter<BigDecimal, Double> {
        @Override
        public Double convert(BigDecimal source) {
            return source != null ? source.doubleValue() : null;
        }
    }
}