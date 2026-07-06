package com.nitrotech.api.application.auth.service;

import com.nitrotech.api.domain.auth.dto.AuthResult;
import com.nitrotech.api.domain.auth.usecase.OAuthCallbackUseCase;
import com.nitrotech.api.shared.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class OAuthLoginRedirectServiceTest {

    private OAuthCallbackUseCase oauthCallbackUseCase;
    private AuthSessionService authSessionService;
    private OAuthStateService oauthStateService;
    private OAuthLoginRedirectService service;

    @BeforeEach
    void setUp() {
        oauthCallbackUseCase = mock(OAuthCallbackUseCase.class);
        authSessionService = mock(AuthSessionService.class);
        oauthStateService = mock(OAuthStateService.class);
        service = new OAuthLoginRedirectService(oauthCallbackUseCase, authSessionService, oauthStateService);
        ReflectionTestUtils.setField(service, "frontendUrl", "http://localhost:3000/account");
    }

    @Test
    void redirectsToFrontendAfterSuccessfulLogin() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        AuthResult result = AuthResult.ofUser(new AuthResult.UserData(
                1L, "OAuth User", "oauth@example.com", Set.of("customer"), Set.of("ORDER_READ_OWN")
        ));
        when(oauthCallbackUseCase.execute("google", "auth-code")).thenReturn(result);

        var uri = service.handleCallback("google", "auth-code", "state-123", null, null, request);

        assertThat(uri).hasToString("http://localhost:3000/account");
        verify(oauthStateService).validateState("google", "state-123", request);
        verify(authSessionService).create(result, request);
    }

    @Test
    void redirectsToLoginWithOauthErrorWhenUseCaseFails() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(oauthCallbackUseCase.execute("google", "bad-code"))
                .thenThrow(new BadRequestException("OAUTH_TOKEN_EXCHANGE_FAILED", "Token exchange failed"));

        var uri = service.handleCallback("google", "bad-code", "state-123", null, null, request);

        assertThat(uri).hasToString("http://localhost:3000/login?oauth_error=OAUTH_TOKEN_EXCHANGE_FAILED");
        verify(oauthStateService).validateState("google", "state-123", request);
        verify(authSessionService, never()).create(any(), any());
    }

    @Test
    void redirectsToLoginWhenProviderReturnsAuthorizationError() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        var uri = service.handleCallback("google", null, null, "access_denied", "User cancelled", request);

        assertThat(uri).hasToString("http://localhost:3000/login?oauth_error=OAUTH_AUTHORIZATION_FAILED");
        verifyNoInteractions(oauthCallbackUseCase, authSessionService, oauthStateService);
    }
}
