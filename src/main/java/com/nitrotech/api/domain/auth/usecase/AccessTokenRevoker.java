package com.nitrotech.api.domain.auth.usecase;

public interface AccessTokenRevoker {
    void revoke(String accessToken);
}
