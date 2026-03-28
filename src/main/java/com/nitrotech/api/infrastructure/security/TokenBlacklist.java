package com.nitrotech.api.infrastructure.security;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class TokenBlacklist {

    private static final String PREFIX = "blacklist:";
    private final StringRedisTemplate redis;

    public TokenBlacklist(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void add(String token, long ttlMs) {
        redis.opsForValue().set(PREFIX + token, "1", Duration.ofMillis(ttlMs));
    }

    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redis.hasKey(PREFIX + token));
    }
}
