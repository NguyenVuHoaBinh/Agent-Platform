package viettel.dac.promptservice.config.elastic;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLContext;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

/**
 * Configuration for Elasticsearch SSL connections
 */
@Component
@Slf4j
public class ElasticsearchSslConfig {

    @Value("${spring.elasticsearch.certificate:}")
    private Resource elasticsearchCertificate;

    @Value("${spring.elasticsearch.use-ssl:false}")
    private boolean useSsl;

    /**
     * Build an SSL context with a custom certificate
     */
    public SSLContext buildSslContext() throws Exception {
        if (!useSsl || elasticsearchCertificate == null || !elasticsearchCertificate.exists()) {
            return null;
        }

        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);

        try (InputStream certificateInputStream = elasticsearchCertificate.getInputStream()) {
            Certificate certificate = factory.generateCertificate(certificateInputStream);
            trustStore.setCertificateEntry("elasticsearchCertificate", certificate);
        }

        SSLContextBuilder sslContextBuilder = SSLContexts.custom()
                .loadTrustMaterial(trustStore, null);

        return sslContextBuilder.build();
    }
}