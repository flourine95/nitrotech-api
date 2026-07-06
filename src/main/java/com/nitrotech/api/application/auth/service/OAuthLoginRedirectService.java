package com.nitrotech.api.application.auth.service;

import com.nitrotech.api.domain.auth.dto.AuthResult;
import com.nitrotech.api.domain.auth.usecase.OAuthCallbackUseCase;
import com.nitrotech.api.shared.exception.DomainException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.Assert;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.annotation.PostConstruct;
import java.net.URI;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAuthLoginRedirectService {

    private final OAuthCallbackUseCase oauthCallbackUseCase;
    private final AuthSessionService authSessionService;
    private final OAuthStateService oauthStateService;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @PostConstruct
    void validateConfig() {
        Assert.hasText(frontendUrl, "FRONTEND_URL must be configured");
    }

    public URI handleCallback(
            String provider,
            String code,
            String state,
            String error,
            String errorDescription,
            HttpServletRequest request
    ) {
        if (error != null && !error.isBlank()) {
            log.warn("OAuth authorization failed for provider {}: {}",
                    provider,
                    errorDescription != null && !errorDescription.isBlank() ? errorDescription : error);
            return failureRedirectUri("OAUTH_AUTHORIZATION_FAILED");
        }

        try {
            oauthStateService.validateState(provider, state, request);
            AuthResult result = oauthCallbackUseCase.execute(provider, code);
            authSessionService.create(result, request);
            return successRedirectUri();
        } catch (DomainException ex) {
            return failureRedirectUri(ex.getCode());
        }
    }

    private URI successRedirectUri() {
        return UriComponentsBuilder.fromUriString(frontendUrl).build(true).toUri();
    }

    private URI failureRedirectUri(String errorCode) {
        return UriComponentsBuilder.fromUriString(frontendUrl)
                .replacePath("/login")
                .replaceQuery(null)
                .queryParam("oauth_error", errorCode)
                .build(true)
                .toUri();
    }
}
