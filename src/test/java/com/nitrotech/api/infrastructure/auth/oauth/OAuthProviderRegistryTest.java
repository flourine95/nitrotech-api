package com.nitrotech.api.infrastructure.auth.oauth;

import com.nitrotech.api.domain.auth.provider.OAuthProvider;
import com.nitrotech.api.shared.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OAuthProviderRegistryTest {

    private OAuthProvider googleProvider;
    private OAuthProvider githubProvider;
    private OAuthProviderRegistry registry;

    @BeforeEach
    void setUp() {
        googleProvider = mock(OAuthProvider.class);
        when(googleProvider.getProviderName()).thenReturn("google");

        githubProvider = mock(OAuthProvider.class);
        when(githubProvider.getProviderName()).thenReturn("github");

        registry = new OAuthProviderRegistry(List.of(googleProvider, githubProvider));
    }

    @Test
    void retrievesProviderByNameCaseInsensitively() {
        assertThat(registry.getProvider("google")).isEqualTo(googleProvider);
        assertThat(registry.getProvider("GOOGLE")).isEqualTo(googleProvider);
        assertThat(registry.getProvider("github")).isEqualTo(githubProvider);
        assertThat(registry.getProvider("GitHub")).isEqualTo(githubProvider);
    }

    @Test
    void throwsExceptionWhenProviderNotFound() {
        assertThatThrownBy(() -> registry.getProvider("facebook"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("OAuth provider 'facebook' is not supported");
    }
}
