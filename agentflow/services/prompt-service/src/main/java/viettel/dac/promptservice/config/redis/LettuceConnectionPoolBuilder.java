package viettel.dac.promptservice.config.redis;

import lombok.RequiredArgsConstructor;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import viettel.dac.promptservice.config.redis.RedisProperties;

import java.time.Duration;

/**
 * Builder for Lettuce connection pool configuration
 */
@RequiredArgsConstructor
public class LettuceConnectionPoolBuilder {
    private final RedisProperties.Pool poolConfig;

    /**
     * Build Lettuce client configuration with connection pooling
     */
    public LettuceClientConfiguration build(Duration commandTimeout, Duration shutdownTimeout) {
        return LettucePoolingClientConfiguration.builder()
                .poolConfig(createPoolConfig())
                .commandTimeout(commandTimeout)
                .shutdownTimeout(shutdownTimeout)
                .build();
    }

    /**
     * Create Apache Commons Pool configuration with the specified settings
     */
    private GenericObjectPoolConfig createPoolConfig() {
        GenericObjectPoolConfig config = new GenericObjectPoolConfig();
        config.setMaxTotal(poolConfig.getMaxTotal());
        config.setMaxIdle(poolConfig.getMaxIdle());
        config.setMinIdle(poolConfig.getMinIdle());
        config.setMaxWait(poolConfig.getMaxWait());
        config.setTestOnBorrow(poolConfig.isTestOnBorrow());
        config.setTestOnReturn(poolConfig.isTestOnReturn());
        config.setTestWhileIdle(poolConfig.isTestWhileIdle());
        return config;
    }
}