package com.nitrotech.api.application.auth.service;

import com.nitrotech.api.domain.auth.usecase.OAuthAuthorizeUseCase;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OAuthAuthorizationService {

    private final OAuthAuthorizeUseCase oauthAuthorizeUseCase;
    private final OAuthStateService oauthStateService;

    public String authorize(String provider, HttpServletRequest request) {
        return oauthAuthorizeUseCase.execute(provider, oauthStateService.createState(provider, request));
    }
}
