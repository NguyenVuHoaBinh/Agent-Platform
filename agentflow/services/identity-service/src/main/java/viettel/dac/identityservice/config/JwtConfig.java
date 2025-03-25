package viettel.dac.identityservice.config;

import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;

@Configuration
@Getter
public class JwtConfig {

    @Value("${security.jwt.secret-key}")
    private String secretKeyString;

    @Value("${security.jwt.access-token-expiration-ms}")
    private long accessTokenExpirationMs;

    @Value("${security.jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    @Value("${security.jwt.mfa-token-expiration-ms}")
    private long mfaTokenExpirationMs;

    @Bean
    public SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKeyString.getBytes(StandardCharsets.UTF_8));
    }

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}