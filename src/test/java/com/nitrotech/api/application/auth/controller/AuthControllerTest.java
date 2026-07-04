package com.nitrotech.api.application.auth.controller;

import com.nitrotech.api.domain.auth.dto.AuthResult;
import com.nitrotech.api.domain.auth.provider.OAuthProviderResolver;
import com.nitrotech.api.domain.auth.usecase.ChangePasswordUseCase;
import com.nitrotech.api.domain.auth.usecase.CreateAuthSessionUseCase;
import com.nitrotech.api.domain.auth.usecase.ForgotPasswordUseCase;
import com.nitrotech.api.domain.auth.usecase.GetProfileUseCase;
import com.nitrotech.api.domain.auth.usecase.LoginUseCase;
import com.nitrotech.api.domain.auth.usecase.LogoutUseCase;
import com.nitrotech.api.domain.auth.usecase.OAuthCallbackUseCase;
import com.nitrotech.api.domain.auth.usecase.RegisterUseCase;
import com.nitrotech.api.domain.auth.usecase.ResendVerificationUseCase;
import com.nitrotech.api.domain.auth.usecase.ResetPasswordUseCase;
import com.nitrotech.api.domain.auth.usecase.UpdateProfileUseCase;
import com.nitrotech.api.domain.auth.usecase.VerifyEmailUseCase;
import com.nitrotech.api.shared.exception.BadRequestException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AuthControllerTest {

    private OAuthCallbackUseCase oauthCallbackUseCase;
    private CreateAuthSessionUseCase createAuthSessionUseCase;
    private AuthController controller;

    @BeforeEach
    void setUp() {
        oauthCallbackUseCase = mock(OAuthCallbackUseCase.class);
        createAuthSessionUseCase = mock(CreateAuthSessionUseCase.class);

        controller = new AuthController(
                mock(RegisterUseCase.class),
                mock(LoginUseCase.class),
                mock(LogoutUseCase.class),
                mock(GetProfileUseCase.class),
                mock(UpdateProfileUseCase.class),
                mock(ChangePasswordUseCase.class),
                mock(ForgotPasswordUseCase.class),
                mock(ResetPasswordUseCase.class),
                mock(VerifyEmailUseCase.class),
                mock(ResendVerificationUseCase.class),
                mock(OAuthProviderResolver.class),
                oauthCallbackUseCase,
                createAuthSessionUseCase
        );
        ReflectionTestUtils.setField(controller, "frontendUrl", "http://localhost:3000/account");
    }

    @Test
    void oauthCallbackRedirectsToFrontendAfterSuccessfulLogin() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        AuthResult result = AuthResult.ofUser(new AuthResult.UserData(
                1L,
                "OAuth User",
                "oauth@example.com",
                Set.of("customer"),
                Set.of("ORDER_READ_OWN")
        ));
        when(oauthCallbackUseCase.execute("google", "auth-code")).thenReturn(result);

        var response = controller.oauthCallback("google", "auth-code", null, null, request);

        assertThat(response.getStatusCode().value()).isEqualTo(302);
        assertThat(response.getHeaders().getLocation()).hasToString("http://localhost:3000/account");
        verify(createAuthSessionUseCase).execute(result, request);
    }

    @Test
    void oauthCallbackRedirectsToLoginWithOauthErrorWhenUseCaseFails() {
        HttpServletRequest request = new MockHttpServletRequest();
        when(oauthCallbackUseCase.execute("google", "bad-code"))
                .thenThrow(new BadRequestException("OAUTH_TOKEN_EXCHANGE_FAILED", "Token exchange failed"));

        var response = controller.oauthCallback("google", "bad-code", null, null, request);

        assertThat(response.getStatusCode().value()).isEqualTo(302);
        assertThat(response.getHeaders().getLocation())
                .hasToString("http://localhost:3000/login?oauth_error=OAUTH_TOKEN_EXCHANGE_FAILED");
        verify(createAuthSessionUseCase, never()).execute(any(), any());
    }

    @Test
    void oauthCallbackRedirectsToLoginWhenProviderReturnsAuthorizationError() {
        HttpServletRequest request = new MockHttpServletRequest();

        var response = controller.oauthCallback("google", null, "access_denied", "User cancelled", request);

        assertThat(response.getStatusCode().value()).isEqualTo(302);
        assertThat(response.getHeaders().getLocation())
                .hasToString("http://localhost:3000/login?oauth_error=OAUTH_AUTHORIZATION_FAILED");
        verifyNoInteractions(oauthCallbackUseCase, createAuthSessionUseCase);
    }
}
