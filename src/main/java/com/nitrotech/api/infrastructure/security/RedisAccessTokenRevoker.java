package com.nitrotech.api.infrastructure.security;

import com.nitrotech.api.domain.auth.usecase.AccessTokenRevoker;
import com.nitrotech.api.domain.auth.usecase.TokenProvider;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class RedisAccessTokenRevoker implements AccessTokenRevoker {

    private final TokenBlacklist tokenBlacklist;
    private final TokenProvider tokenProvider;

    public RedisAccessTokenRevoker(TokenBlacklist tokenBlacklist, TokenProvider tokenProvider) {
        this.tokenBlacklist = tokenBlacklist;
        this.tokenProvider = tokenProvider;
    }

    @Override
    public void revoke(String accessToken) {
        long ttl = tokenProvider.getExpiration(accessToken).getTime() - System.currentTimeMillis();
        if (ttl > 0) {
            tokenBlacklist.add(accessToken, ttl);
        }
    }
}
