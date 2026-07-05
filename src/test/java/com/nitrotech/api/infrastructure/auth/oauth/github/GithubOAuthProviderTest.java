package com.nitrotech.api.infrastructure.auth.oauth.github;

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

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GithubOAuthProviderTest {

    private GithubOAuthProvider provider;
    private MockRestServiceServer mockServer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        provider = new GithubOAuthProvider(builder, new GithubOAuthProperties(
                "github-client-id",
                "github-client-secret",
                "http://localhost:8080/api/auth/oauth/github/callback"
        ));
    }

    @Test
    void buildsAuthorizationUrlWithExpectedQueryParams() {
        String url = provider.buildAuthorizationUrl();
        var params = UriComponentsBuilder.fromUriString(url).build().getQueryParams();

        assertThat(params.getFirst("client_id")).isEqualTo("github-client-id");
        assertThat(params.getFirst("redirect_uri")).isEqualTo("http://localhost:8080/api/auth/oauth/github/callback");
        assertThat(params.getFirst("scope")).isEqualTo("read:user user:email");
    }

    @Test
    void exchangesCodeAndFetchesVerifiedPrimaryEmail() throws Exception {
        mockServer.expect(requestTo("https://github.com/login/oauth/access_token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Content-Type", containsString(MediaType.APPLICATION_FORM_URLENCODED_VALUE)))
                .andExpect(header("Accept", containsString(MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(content().string(containsString("code=github-code")))
                .andExpect(content().string(containsString("client_id=github-client-id")))
                .andRespond(withSuccess(objectMapper.writeValueAsString(Map.of(
                        "access_token", "github-access-token",
                        "token_type", "bearer",
                        "scope", "read:user,user:email"
                )), MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo("https://api.github.com/user"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer github-access-token"))
                .andRespond(withSuccess(objectMapper.writeValueAsString(Map.of(
                        "id", 42,
                        "login", "octocat",
                        "name", "Octo Cat",
                        "avatar_url", "https://example.com/octocat.png"
                )), MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo("https://api.github.com/user/emails"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer github-access-token"))
                .andRespond(withSuccess(objectMapper.writeValueAsString(List.of(
                        Map.of("email", "secondary@example.com", "primary", false, "verified", true),
                        Map.of("email", "primary@example.com", "primary", true, "verified", true)
                )), MediaType.APPLICATION_JSON));

        OAuthTokenResponse token = provider.exchangeAuthorizationCode("github-code");
        OAuthUserInfo userInfo = provider.fetchUserInfo(token);

        assertThat(token.accessToken()).isEqualTo("github-access-token");
        assertThat(userInfo.externalId()).isEqualTo("42");
        assertThat(userInfo.email()).isEqualTo("primary@example.com");
        assertThat(userInfo.name()).isEqualTo("Octo Cat");
        assertThat(userInfo.emailVerified()).isTrue();

        mockServer.verify();
    }
}
