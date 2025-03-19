package viettel.dac.promptservice.config.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * Configuration for Redis connection using Lettuce client
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = true)
public class RedisConnectionConfig {
    private final RedisProperties redisProperties;

    /**
     * Create the Redis connection factory with appropriate connection settings
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
        redisConfig.setHostName(redisProperties.getHost());
        redisConfig.setPort(redisProperties.getPort());
        redisConfig.setDatabase(redisProperties.getDatabase());

        if (StringUtils.hasText(redisProperties.getPassword())) {
            redisConfig.setPassword(RedisPassword.of(redisProperties.getPassword()));
        }

        LettuceConnectionPoolBuilder poolBuilder = new LettuceConnectionPoolBuilder(redisProperties.getLettuce());
        LettuceClientConfiguration clientConfig = poolBuilder.build(
                Duration.ofMillis(redisProperties.getTimeout()),
                Duration.ofSeconds(5)
        );

        LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(redisConfig, clientConfig);
        connectionFactory.setValidateConnection(true);

        log.info("Configured Redis connection to {}:{} (database: {})",
                redisProperties.getHost(),
                redisProperties.getPort(),
                redisProperties.getDatabase());

        return connectionFactory;
    }
}