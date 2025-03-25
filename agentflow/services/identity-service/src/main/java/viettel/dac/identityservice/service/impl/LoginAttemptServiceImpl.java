package viettel.dac.identityservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import viettel.dac.identityservice.common.constants.SecurityConstants;
import viettel.dac.identityservice.service.LoginAttemptService;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class LoginAttemptServiceImpl implements LoginAttemptService {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String ATTEMPTS_PREFIX = "login:attempts:";
    private static final String BLOCKED_PREFIX = "login:blocked:";

    @Override
    public void recordFailedAttempt(String usernameOrEmail) {
        String attemptsKey = ATTEMPTS_PREFIX + usernameOrEmail;
        String currentValue = redisTemplate.opsForValue().get(attemptsKey);
        int attempts = 1;

        if (currentValue != null) {
            attempts = Integer.parseInt(currentValue) + 1;
        }

        redisTemplate.opsForValue().set(attemptsKey, String.valueOf(attempts), 1, TimeUnit.DAYS);

        // If max attempts reached, block the account
        if (attempts >= SecurityConstants.MAX_LOGIN_ATTEMPTS) {
            String blockedKey = BLOCKED_PREFIX + usernameOrEmail;
            redisTemplate.opsForValue().set(
                    blockedKey,
                    "1",
                    SecurityConstants.LOGIN_ATTEMPT_LOCKOUT_MINUTES,
                    TimeUnit.MINUTES
            );
        }
    }

    @Override
    public void resetFailedAttempts(String usernameOrEmail) {
        String attemptsKey = ATTEMPTS_PREFIX + usernameOrEmail;
        redisTemplate.delete(attemptsKey);
    }

    @Override
    public boolean isBlocked(String usernameOrEmail) {
        String blockedKey = BLOCKED_PREFIX + usernameOrEmail;
        return Boolean.TRUE.equals(redisTemplate.hasKey(blockedKey));
    }

    @Override
    public int getFailedAttempts(String usernameOrEmail) {
        String attemptsKey = ATTEMPTS_PREFIX + usernameOrEmail;
        String value = redisTemplate.opsForValue().get(attemptsKey);
        return value != null ? Integer.parseInt(value) : 0;
    }
}