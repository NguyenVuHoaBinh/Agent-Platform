package viettel.dac.promptservice.config.cache;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.data.redis.RedisConnectionFailureException;

/**
 * Custom error handler for Redis cache operations with metrics tracking
 */
@Slf4j
public class MetricsEnabledCacheErrorHandler extends SimpleCacheErrorHandler {
    private final MeterRegistry meterRegistry;

    public MetricsEnabledCacheErrorHandler(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        logAndIncrementMetric("get", cache.getName(), key, exception);
        // Only propagate non-connection exceptions to increase resilience
        if (!(exception instanceof RedisConnectionFailureException)) {
            super.handleCacheGetError(exception, cache, key);
        }
    }

    @Override
    public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
        logAndIncrementMetric("put", cache.getName(), key, exception);
        super.handleCachePutError(exception, cache, key, value);
    }

    @Override
    public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
        logAndIncrementMetric("evict", cache.getName(), key, exception);
        super.handleCacheEvictError(exception, cache, key);
    }

    @Override
    public void handleCacheClearError(RuntimeException exception, Cache cache) {
        logAndIncrementMetric("clear", cache.getName(), null, exception);
        super.handleCacheClearError(exception, cache);
    }

    private void logAndIncrementMetric(String operation, String cacheName, Object key, Exception exception) {
        String keyStr = key != null ? key.toString() : "all";
        log.warn("Cache {} operation on cache '{}' for key '{}' failed: {}",
                operation, cacheName, keyStr, exception.getMessage());

        meterRegistry.counter("cache.operations.errors",
                "operation", operation,
                "cache", cacheName,
                "exception", exception.getClass().getSimpleName()).increment();
    }
}