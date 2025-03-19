package viettel.dac.identityservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Configuration for JWT security with vault integration in production
 */
@Configuration
@Validated
public class JwtConfig {

    @Value("${spring.security.jwt.expiration:86400000}")
    private long jwtExpirationMs;

    /**
     * Development JWT key using hardcoded default from properties
     * Only used in dev and test environments
     */
    @Bean
    @Profile({"dev", "test"})
    public SecretKey jwtSecretKeyDev(@Value("${spring.security.jwt.secret}") String jwtSecret) {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    /**
     * Production JWT key that should be retrieved from vault
     * In a real implementation, this would use Spring Vault or similar to retrieve the secret
     */
    @Bean
    @Profile({"staging", "prod"})
    public SecretKey jwtSecretKeyProd(
            @Value("${spring.cloud.vault.token:}") String vaultToken,
            @Value("${spring.cloud.vault.scheme:http}") String vaultScheme,
            @Value("${spring.cloud.vault.host:localhost}") String vaultHost,
            @Value("${spring.cloud.vault.port:8200}") String vaultPort) {

        // In a real implementation, this would fetch from Vault
        // For now, generate a random key for demo purposes
        // This simulates fetching from an external secrets manager
        byte[] keyBytes = new byte[32]; // 256 bits
        new SecureRandom().nextBytes(keyBytes);

        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    /**
     * Stronger password encoder for production (compared to default)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        // Use BCrypt with higher strength (12 rounds instead of default 10)
        return new BCryptPasswordEncoder(12);
    }

    /**
     * JWT token expiration getter
     */
    public long getJwtExpirationMs() {
        return jwtExpirationMs;
    }
}