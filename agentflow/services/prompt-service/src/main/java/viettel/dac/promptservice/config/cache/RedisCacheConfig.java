package viettel.dac.promptservice.config.cache;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import viettel.dac.promptservice.config.redis.RedisProperties;
import viettel.dac.promptservice.config.redis.RedisTemplateConfig;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Configuration for Redis caching with Spring Cache abstraction
 */
@Configuration
@EnableCaching
@RequiredArgsConstructor
@Slf4j
@ConditionalOnBean(RedisConnectionFactory.class)
public class RedisCacheConfig implements CachingConfigurer {
    private final RedisProperties redisProperties;
    private final MeterRegistry meterRegistry;
    private final GenericJackson2JsonRedisSerializer jsonRedisSerializer;

    /**
     * Configure Redis Cache Manager with appropriate TTL settings for different caches
     */
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // Configure specific cache TTLs from properties
        redisProperties.getCache().getTtls().forEach((cacheName, ttl) -> {
            cacheConfigurations.put(cacheName, buildCacheConfig(ttl));
            log.debug("Configured cache '{}' with TTL: {}", cacheName, ttl);
        });

        // Default cache configuration
        RedisCacheConfiguration defaultCacheConfig = buildCacheConfig(
                Duration.ofSeconds(redisProperties.getCache().getTimeToLive()));

        log.info("Configured Redis cache manager with {} specific cache configurations",
                cacheConfigurations.size());

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultCacheConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }

    /**
     * Configure a custom key generator for cache operations
     */
    @Override
    @Bean
    public KeyGenerator keyGenerator() {
        return (target, method, params) -> {
            StringBuilder sb = new StringBuilder();
            sb.append(target.getClass().getSimpleName()).append(".");
            sb.append(method.getName()).append(".");

            for (Object param : params) {
                sb.append(Objects.nonNull(param) ? param.toString() : "null").append(".");
            }

            return sb.toString();
        };
    }

    /**
     * Configure error handling for cache operations
     */
    @Override
    @Bean
    public CacheErrorHandler errorHandler() {
        return new MetricsEnabledCacheErrorHandler(meterRegistry);
    }

    /**
     * Build a cache configuration with the specified TTL
     */
    private RedisCacheConfiguration buildCacheConfig(Duration ttl) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(ttl)
                .prefixCacheNameWith(redisProperties.getCache().getKeyPrefix())
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(jsonRedisSerializer))
                .disableCachingNullValues();
    }
}