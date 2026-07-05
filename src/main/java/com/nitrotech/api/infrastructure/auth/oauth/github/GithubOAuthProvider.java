package com.nitrotech.api.infrastructure.auth.oauth.github;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nitrotech.api.domain.auth.dto.OAuthTokenResponse;
import com.nitrotech.api.domain.auth.dto.OAuthUserInfo;
import com.nitrotech.api.domain.auth.provider.OAuthProvider;
import com.nitrotech.api.shared.exception.BadRequestException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;

@Component
@EnableConfigurationProperties(GithubOAuthProperties.class)
public class GithubOAuthProvider implements OAuthProvider {

    private final RestClient authClient;
    private final RestClient apiClient;
    private final GithubOAuthProperties properties;

    public GithubOAuthProvider(RestClient.Builder builder, GithubOAuthProperties properties) {
        this.authClient = builder.baseUrl("https://github.com").build();
        this.apiClient = builder.baseUrl("https://api.github.com").build();
        this.properties = properties;
    }

    @Override
    public String getProviderName() {
        return "github";
    }

    @Override
    public String buildAuthorizationUrl() {
        validateConfigured();
        return UriComponentsBuilder.fromUriString("https://github.com/login/oauth/authorize")
                .queryParam("client_id", properties.clientId())
                .queryParam("redirect_uri", properties.redirectUri())
                .queryParam("scope", "read:user user:email")
                .build()
                .toUriString();
    }

    @Override
    public OAuthTokenResponse exchangeAuthorizationCode(String code) {
        validateConfigured();
        try {
            GithubTokenResponse response = authClient.post()
                    .uri("/login/oauth/access_token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(tokenRequestBody(code))
                    .retrieve()
                    .body(GithubTokenResponse.class);
            if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
                throw new BadRequestException("OAUTH_TOKEN_EXCHANGE_FAILED", "GitHub OAuth token exchange returned no access token");
            }
            return new OAuthTokenResponse(response.accessToken(), response.tokenType(), response.scope());
        } catch (RestClientException ex) {
            throw new BadRequestException("OAUTH_TOKEN_EXCHANGE_FAILED", "GitHub OAuth token exchange failed");
        }
    }

    @Override
    public OAuthUserInfo fetchUserInfo(OAuthTokenResponse tokenResponse) {
        try {
            GithubUserResponse user = apiClient.get()
                    .uri("/user")
                    .headers(headers -> headers.setBearerAuth(tokenResponse.accessToken()))
                    .headers(headers -> headers.set("Accept", MediaType.APPLICATION_JSON_VALUE))
                    .retrieve()
                    .body(GithubUserResponse.class);

            if (user == null || user.id() == null) {
                throw new BadRequestException("OAUTH_USER_INFO_INVALID", "GitHub OAuth user info is invalid");
            }

            GithubEmailResponse[] emails = apiClient.get()
                    .uri("/user/emails")
                    .headers(headers -> headers.setBearerAuth(tokenResponse.accessToken()))
                    .headers(headers -> headers.set("Accept", MediaType.APPLICATION_JSON_VALUE))
                    .retrieve()
                    .body(GithubEmailResponse[].class);

            GithubEmailResponse primaryEmail = emails == null ? null : Arrays.stream(emails)
                    .filter(email -> Boolean.TRUE.equals(email.verified()))
                    .sorted((left, right) -> Boolean.compare(Boolean.TRUE.equals(right.primary()), Boolean.TRUE.equals(left.primary())))
                    .findFirst()
                    .orElse(null);

            String email = primaryEmail != null ? primaryEmail.email() : user.email();
            boolean emailVerified = primaryEmail != null && Boolean.TRUE.equals(primaryEmail.verified());
            if (email == null || email.isBlank()) {
                throw new BadRequestException("OAUTH_EMAIL_REQUIRED", "GitHub OAuth did not return a verified email address");
            }

            return new OAuthUserInfo(
                    String.valueOf(user.id()),
                    email,
                    user.name() == null || user.name().isBlank() ? user.login() : user.name(),
                    user.avatarUrl(),
                    emailVerified
            );
        } catch (RestClientException ex) {
            throw new BadRequestException("OAUTH_USER_INFO_FAILED", "GitHub OAuth user info request failed");
        }
    }

    private MultiValueMap<String, String> tokenRequestBody(String code) {
        LinkedMultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", code);
        body.add("client_id", properties.clientId());
        body.add("client_secret", properties.clientSecret());
        body.add("redirect_uri", properties.redirectUri());
        return body;
    }

    private void validateConfigured() {
        if (isBlank(properties.clientId()) || isBlank(properties.clientSecret()) || isBlank(properties.redirectUri())) {
            throw new BadRequestException("OAUTH_PROVIDER_NOT_CONFIGURED", "OAuth provider 'github' is not configured.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record GithubTokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("token_type") String tokenType,
            String scope
    ) {
    }

    private record GithubUserResponse(
            Long id,
            String login,
            String name,
            String email,
            @JsonProperty("avatar_url") String avatarUrl
    ) {
    }

    private record GithubEmailResponse(String email, Boolean primary, Boolean verified) {
    }
}
