package viettel.dac.identityservice.security;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Map;

@Configuration
public class JwksConfig {

    @Value("${spring.security.jwt.keyId:agentflow-key-id}")
    private String keyId;

    private RSAKey rsaKey;

    @Bean
    public KeyPair keyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            return keyPairGenerator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate RSA key pair", ex);
        }
    }

    @Bean
    public RSAKey rsaKey(KeyPair keyPair) {
        this.rsaKey = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey((RSAPrivateKey) keyPair.getPrivate())
                .keyID(keyId)
                .build();
        return this.rsaKey;
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource(RSAKey rsaKey) {
        JWKSet jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
    }

    // Controller for the public JWKS endpoint
    @RestController
    @Order(1)
    public class JwkSetController {
        private final RSAKey rsaKey;

        public JwkSetController(RSAKey rsaKey) {
            this.rsaKey = rsaKey;
        }

        @GetMapping("/.well-known/jwks.json")
        public Map<String, Object> jwks() {
            try {
                // Create a JWK set with the public key only
                RSAKey publicRsaKey = new RSAKey.Builder(rsaKey.toRSAPublicKey())
                        .keyID(rsaKey.getKeyID())
                        .build();

                return new JWKSet(publicRsaKey).toJSONObject();
            } catch (com.nimbusds.jose.JOSEException e) {
                throw new RuntimeException("Error creating JWKS", e);
            }
        }
    }
}