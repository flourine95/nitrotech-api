package com.nitrotech.api.infrastructure.auth.oauth.google;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nitrotech.api.domain.auth.dto.OAuthTokenResponse;
import com.nitrotech.api.domain.auth.dto.OAuthUserInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GoogleOAuthProviderTest {

    private GoogleOAuthProvider provider;
    private MockRestServiceServer mockServer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        provider = new GoogleOAuthProvider(builder, new GoogleOAuthProperties(
                "google-client-id",
                "google-client-secret",
                "http://localhost:8080/api/auth/oauth/google/callback"
        ), false);
    }

    @Test
    void buildsAuthorizationUrlWithExpectedQueryParams() {
        String url = provider.buildAuthorizationUrl("state-123");
        var params = UriComponentsBuilder.fromUriString(url).build().getQueryParams();

        assertThat(params.getFirst("client_id")).isEqualTo("google-client-id");
        assertThat(params.getFirst("redirect_uri")).isEqualTo("http://localhost:8080/api/auth/oauth/google/callback");
        assertThat(params.getFirst("response_type")).isEqualTo("code");
        assertThat(params.getFirst("scope")).isEqualTo("openid email profile");
        assertThat(params.getFirst("state")).isEqualTo("state-123");
    }

    @Test
    void exchangesCodeAndFetchesUserInfo() throws Exception {
        mockServer.expect(requestTo("https://oauth2.googleapis.com/token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Content-Type", containsString(MediaType.APPLICATION_FORM_URLENCODED_VALUE)))
                .andExpect(content().string(containsString("code=auth-code")))
                .andExpect(content().string(containsString("client_id=google-client-id")))
                .andExpect(content().string(containsString("client_secret=google-client-secret")))
                .andRespond(withSuccess(objectMapper.writeValueAsString(Map.of(
                        "access_token", "google-access-token",
                        "token_type", "Bearer",
                        "scope", "openid email profile"
                )), MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo("https://www.googleapis.com/oauth2/v2/userinfo"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer google-access-token"))
                .andRespond(withSuccess(objectMapper.writeValueAsString(Map.of(
                        "id", "google-user-1",
                        "email", "google@example.com",
                        "name", "Google User",
                        "picture", "https://example.com/google-avatar.png",
                        "verified_email", true
                )), MediaType.APPLICATION_JSON));

        OAuthTokenResponse token = provider.exchangeAuthorizationCode("auth-code");
        OAuthUserInfo userInfo = provider.fetchUserInfo(token);

        assertThat(token.accessToken()).isEqualTo("google-access-token");
        assertThat(userInfo.externalId()).isEqualTo("google-user-1");
        assertThat(userInfo.email()).isEqualTo("google@example.com");
        assertThat(userInfo.emailVerified()).isTrue();

        mockServer.verify();
    }
}
