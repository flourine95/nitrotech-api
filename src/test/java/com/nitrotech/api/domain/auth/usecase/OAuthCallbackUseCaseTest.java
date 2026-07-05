package com.nitrotech.api.domain.auth.usecase;

import com.nitrotech.api.domain.auth.dto.AuthResult;
import com.nitrotech.api.domain.auth.dto.OAuthTokenResponse;
import com.nitrotech.api.domain.auth.dto.OAuthUserInfo;
import com.nitrotech.api.domain.auth.exception.AccountNotActiveException;
import com.nitrotech.api.domain.auth.provider.OAuthProvider;
import com.nitrotech.api.domain.auth.provider.OAuthProviderResolver;
import com.nitrotech.api.domain.auth.repository.OAuthAccountRepository;
import com.nitrotech.api.domain.auth.repository.UserRepository;
import com.nitrotech.api.shared.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class OAuthCallbackUseCaseTest {

    private OAuthProviderResolver resolver;
    private OAuthAccountRepository oauthAccountRepository;
    private UserRepository userRepository;
    private OAuthProvider provider;
    private OAuthCallbackUseCase useCase;

    @BeforeEach
    void setUp() {
        resolver = mock(OAuthProviderResolver.class);
        oauthAccountRepository = mock(OAuthAccountRepository.class);
        userRepository = mock(UserRepository.class);
        provider = mock(OAuthProvider.class);

        when(provider.getProviderName()).thenReturn("google");
        when(resolver.getProvider("google")).thenReturn(provider);
        when(provider.exchangeAuthorizationCode("auth-code"))
                .thenReturn(new OAuthTokenResponse("access-token", "Bearer", "openid email profile"));
        when(provider.fetchUserInfo(any()))
                .thenReturn(new OAuthUserInfo(
                        "google-user-1",
                        "google@example.com",
                        "Google User",
                        "https://example.com/avatar.png",
                        true
                ));

        useCase = new OAuthCallbackUseCase(resolver, oauthAccountRepository, userRepository);
    }

    @Test
    void createsAndLinksUserWhenEmailDoesNotExist() {
        when(oauthAccountRepository.findByProviderAndExternalId("google", "google-user-1"))
                .thenReturn(Optional.empty());
        when(userRepository.findAuthAccountByEmail("google@example.com"))
                .thenReturn(Optional.empty());
        when(userRepository.saveOAuthUser(
                "Google User",
                "google@example.com",
                "https://example.com/avatar.png",
                "google",
                "google-user-1"
        )).thenReturn(new UserRepository.UserAuthAccount(10L, "Google User", "google@example.com", "active"));
        when(userRepository.findAuthoritiesByUserId(10L))
                .thenReturn(new UserRepository.UserAuthorities(Set.of("customer"), Set.of("ORDER_CREATE")));

        AuthResult result = useCase.execute("google", "auth-code");

        assertThat(result.user().id()).isEqualTo(10L);
        assertThat(result.user().email()).isEqualTo("google@example.com");
        verify(oauthAccountRepository).saveOrUpdate(10L, "google", new OAuthUserInfo(
                "google-user-1",
                "google@example.com",
                "Google User",
                "https://example.com/avatar.png",
                true
        ));
    }

    @Test
    void activatesAndLinksExistingInactiveUser() {
        when(oauthAccountRepository.findByProviderAndExternalId("google", "google-user-1"))
                .thenReturn(Optional.empty());
        when(userRepository.findAuthAccountByEmail("google@example.com"))
                .thenReturn(Optional.of(new UserRepository.UserAuthAccount(11L, "Existing User", "google@example.com", "inactive")));
        when(oauthAccountRepository.findByProviderAndUserId("google", 11L))
                .thenReturn(Optional.empty());
        when(userRepository.findAuthoritiesByUserId(11L))
                .thenReturn(new UserRepository.UserAuthorities(Set.of("customer"), Set.of()));

        AuthResult result = useCase.execute("google", "auth-code");

        assertThat(result.user().id()).isEqualTo(11L);
        verify(oauthAccountRepository).saveOrUpdate(11L, "google", new OAuthUserInfo(
                "google-user-1",
                "google@example.com",
                "Google User",
                "https://example.com/avatar.png",
                true
        ));
        verify(userRepository).activateUser(11L);
    }

    @Test
    void rejectsDifferentGoogleAccountWhenProviderAlreadyLinked() {
        when(oauthAccountRepository.findByProviderAndExternalId("google", "google-user-1"))
                .thenReturn(Optional.empty());
        when(userRepository.findAuthAccountByEmail("google@example.com"))
                .thenReturn(Optional.of(new UserRepository.UserAuthAccount(11L, "Existing User", "google@example.com", "active")));
        when(oauthAccountRepository.findByProviderAndUserId("google", 11L))
                .thenReturn(Optional.of(new OAuthAccountRepository.OAuthAccountLink(11L, "google", "another-google-user")));

        assertThatThrownBy(() -> useCase.execute("google", "auth-code"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Another google account is already linked");
    }

    @Test
    void rejectsBannedUserEvenWhenOAuthAccountExists() {
        when(oauthAccountRepository.findByProviderAndExternalId("google", "google-user-1"))
                .thenReturn(Optional.of(new OAuthAccountRepository.OAuthAccountLink(22L, "google", "google-user-1")));
        when(userRepository.findAuthAccountById(22L))
                .thenReturn(Optional.of(new UserRepository.UserAuthAccount(22L, "Blocked User", "google@example.com", "banned")));

        assertThatThrownBy(() -> useCase.execute("google", "auth-code"))
                .isInstanceOf(AccountNotActiveException.class);
    }
}
