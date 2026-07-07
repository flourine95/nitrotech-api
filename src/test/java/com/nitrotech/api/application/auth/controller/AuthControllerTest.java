package com.nitrotech.api.application.auth.controller;

import com.nitrotech.api.application.auth.service.AuthSessionService;
import com.nitrotech.api.application.auth.service.OAuthAuthorizationService;
import com.nitrotech.api.application.auth.service.OAuthLoginRedirectService;
import com.nitrotech.api.domain.auth.dto.AuthResult;
import com.nitrotech.api.domain.auth.dto.UserProfileData;
import com.nitrotech.api.domain.auth.usecase.*;
import com.nitrotech.api.shared.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.net.URI;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AuthControllerTest {

    private OAuthAuthorizationService oauthAuthorizationService;
    private AuthSessionService authSessionService;
    private OAuthLoginRedirectService oauthLoginRedirectService;
    private GetProfileUseCase getProfileUseCase;
    private AuthController controller;

    @BeforeEach
    void setUp() {
        oauthAuthorizationService = mock(OAuthAuthorizationService.class);
        authSessionService = mock(AuthSessionService.class);
        oauthLoginRedirectService = mock(OAuthLoginRedirectService.class);
        getProfileUseCase = mock(GetProfileUseCase.class);

        controller = new AuthController(
                mock(RegisterUseCase.class),
                mock(LoginUseCase.class),
                mock(LogoutUseCase.class),
                getProfileUseCase,
                mock(UpdateProfileUseCase.class),
                mock(ChangePasswordUseCase.class),
                mock(ForgotPasswordUseCase.class),
                mock(ResetPasswordUseCase.class),
                mock(VerifyEmailUseCase.class),
                mock(ResendVerificationUseCase.class),
                oauthAuthorizationService,
                authSessionService,
                oauthLoginRedirectService
        );
    }

    @Test
    void authorizeOAuthReturnsProviderAuthorizationUrl() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(oauthAuthorizationService.authorize("github", request)).thenReturn("https://github.com/login/oauth/authorize");

        var response = controller.authorizeOAuth("github", request);

        assertThat(response.getBody().data().get("authorizationUrl"))
                .isEqualTo("https://github.com/login/oauth/authorize");
    }

    @Test
    void loginCreatesSession() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        LoginUseCase loginUseCase = mock(LoginUseCase.class);
        AuthResult result = AuthResult.ofUser(new AuthResult.UserData(
                1L, "User", "user@example.com", Set.of("customer"), Set.of("ORDER_READ_OWN")
        ));
        when(loginUseCase.execute(any())).thenReturn(result);

        controller = new AuthController(
                mock(RegisterUseCase.class),
                loginUseCase,
                mock(LogoutUseCase.class),
                mock(GetProfileUseCase.class),
                mock(UpdateProfileUseCase.class),
                mock(ChangePasswordUseCase.class),
                mock(ForgotPasswordUseCase.class),
                mock(ResetPasswordUseCase.class),
                mock(VerifyEmailUseCase.class),
                mock(ResendVerificationUseCase.class),
                oauthAuthorizationService,
                authSessionService,
                oauthLoginRedirectService
        );

        controller.login(new com.nitrotech.api.application.auth.request.LoginRequest("user@example.com", "secret"), request);

        verify(authSessionService).create(result, request);
    }

    @Test
    void meReturnsCurrentRolesAndPermissions() {
        UserPrincipal principal = new UserPrincipal(
                1L, "admin@example.com", "Admin", Set.of("admin"), Set.of("ORDER_READ_ALL")
        );
        UserProfileData profile = new UserProfileData(
                1L, "Admin", "admin@example.com", null, null, "active", "local",
                Set.of("admin"), Set.of("ORDER_READ_ALL")
        );
        when(getProfileUseCase.execute(1L)).thenReturn(profile);

        var response = controller.me(principal);

        assertThat(response.getBody().data().roles()).containsExactly("admin");
        assertThat(response.getBody().data().permissions()).containsExactly("ORDER_READ_ALL");
    }

    @Test
    void oauthCallbackRedirectsToServiceResult() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(oauthLoginRedirectService.handleCallback("google", "code", "state-123", null, null, request))
                .thenReturn(URI.create("http://localhost:3000"));

        var response = controller.oauthCallback("google", "code", "state-123", null, null, request);

        assertThat(response.getStatusCode().value()).isEqualTo(302);
        assertThat(response.getHeaders().getLocation()).hasToString("http://localhost:3000");
    }
}
