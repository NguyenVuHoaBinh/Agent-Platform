package viettel.dac.promptservice.config.redis;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "spring.redis")
@Getter
@Setter
public class RedisProperties {
    private String host = "localhost";
    private int port = 6379;
    private String password = "";
    private int database = 0;
    private int timeout = 2000;
    private boolean ssl = false;

    @NestedConfigurationProperty
    private Pool lettuce = new Pool();

    @NestedConfigurationProperty
    private Cache cache = new Cache();

    @Getter
    @Setter
    public static class Pool {
        private int maxTotal = 8;
        private int maxIdle = 8;
        private int minIdle = 0;
        private Duration maxWait = Duration.ofMillis(-1);
        private boolean testOnBorrow = false;
        private boolean testOnReturn = false;
        private boolean testWhileIdle = false;
    }

    @Getter
    @Setter
    public static class Cache {
        private long timeToLive = 3600;
        private String keyPrefix = "promptservice:";
        private Map<String, Duration> ttls = new HashMap<>();

        // Initialize with default cache TTLs
        public Cache() {
            ttls.put("promptTemplates", Duration.ofHours(12));
            ttls.put("promptVersions", Duration.ofHours(6));
            ttls.put("promptParameters", Duration.ofHours(6));
            ttls.put("searchResults", Duration.ofMinutes(30));
            ttls.put("userPermissions", Duration.ofMinutes(15));
        }
    }
}