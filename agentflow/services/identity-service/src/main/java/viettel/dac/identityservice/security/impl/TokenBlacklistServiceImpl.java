package viettel.dac.identityservice.security.impl;

import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import viettel.dac.identityservice.config.JwtConfig;
import viettel.dac.identityservice.security.TokenBlacklistService;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TokenBlacklistServiceImpl implements TokenBlacklistService {

    private final RedisTemplate<String, String> redisTemplate;
    private final JwtConfig jwtConfig;

    private static final String BLACKLIST_PREFIX = "token:blacklist:";

    @Override
    public void blacklistToken(String tokenId, Date expiration) {
        long ttl = expiration.getTime() - System.currentTimeMillis();

        // Only add to blacklist if not expired
        if (ttl > 0) {
            redisTemplate.opsForValue().set(
                    BLACKLIST_PREFIX + tokenId,
                    "1",
                    ttl,
                    TimeUnit.MILLISECONDS
            );
        }
    }

    @Override
    public boolean isBlacklisted(String token) {
        try {
            String tokenId = Jwts.parserBuilder()
                    .setSigningKey(jwtConfig.getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getId();

            return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + tokenId));
        } catch (Exception e) {
            // If we can't parse the token, consider it invalid/blacklisted
            return true;
        }
    }

    /**
     * Scheduled job to clean up expired blacklist entries
     * This is a backup in case Redis TTL doesn't clean up properly
     */
    @Scheduled(cron = "0 0 0 * * ?") // Run daily at midnight
    public void cleanupExpiredBlacklist() {
        // Redis handles expiration automatically via TTL
        // This method exists as a placeholder in case additional cleanup is needed
    }
}