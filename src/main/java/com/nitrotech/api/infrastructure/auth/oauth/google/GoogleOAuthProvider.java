package com.nitrotech.api.infrastructure.auth.oauth.google;

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

@Component
@EnableConfigurationProperties(GoogleOAuthProperties.class)
public class GoogleOAuthProvider implements OAuthProvider {

    private final RestClient restClient;
    private final GoogleOAuthProperties properties;

    public GoogleOAuthProvider(RestClient.Builder builder, GoogleOAuthProperties properties) {
        this.restClient = builder.baseUrl("https://oauth2.googleapis.com").build();
        this.properties = properties;
    }

    @Override
    public String getProviderName() {
        return "google";
    }

    @Override
    public String buildAuthorizationUrl() {
        validateConfigured();
        return UriComponentsBuilder.fromUriString("https://accounts.google.com/o/oauth2/v2/auth")
                .queryParam("client_id", properties.clientId())
                .queryParam("redirect_uri", properties.redirectUri())
                .queryParam("response_type", "code")
                .queryParam("scope", "openid email profile")
                .queryParam("access_type", "offline")
                .build()
                .toUriString();
    }

    @Override
    public OAuthTokenResponse exchangeAuthorizationCode(String code) {
        validateConfigured();
        try {
            GoogleTokenResponse response = restClient.post()
                    .uri("/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(tokenRequestBody(code))
                    .retrieve()
                    .body(GoogleTokenResponse.class);
            if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
                throw new BadRequestException("OAUTH_TOKEN_EXCHANGE_FAILED", "Google OAuth token exchange returned no access token");
            }
            return new OAuthTokenResponse(response.accessToken(), response.tokenType(), response.scope());
        } catch (RestClientException ex) {
            throw new BadRequestException("OAUTH_TOKEN_EXCHANGE_FAILED", "Google OAuth token exchange failed");
        }
    }

    @Override
    public OAuthUserInfo fetchUserInfo(OAuthTokenResponse tokenResponse) {
        try {
            GoogleUserInfoResponse response = restClient.get()
                    .uri("https://www.googleapis.com/oauth2/v2/userinfo")
                    .headers(headers -> headers.setBearerAuth(tokenResponse.accessToken()))
                    .retrieve()
                    .body(GoogleUserInfoResponse.class);
            if (response == null || response.id() == null || response.id().isBlank()) {
                throw new BadRequestException("OAUTH_USER_INFO_INVALID", "Google OAuth user info is invalid");
            }
            return new OAuthUserInfo(
                    response.id(),
                    response.email(),
                    response.name(),
                    response.picture(),
                    Boolean.TRUE.equals(response.verifiedEmail())
            );
        } catch (RestClientException ex) {
            throw new BadRequestException("OAUTH_USER_INFO_FAILED", "Google OAuth user info request failed");
        }
    }

    private MultiValueMap<String, String> tokenRequestBody(String code) {
        LinkedMultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", code);
        body.add("client_id", properties.clientId());
        body.add("client_secret", properties.clientSecret());
        body.add("redirect_uri", properties.redirectUri());
        body.add("grant_type", "authorization_code");
        return body;
    }

    private void validateConfigured() {
        if (isBlank(properties.clientId()) || isBlank(properties.clientSecret()) || isBlank(properties.redirectUri())) {
            throw new BadRequestException("OAUTH_PROVIDER_NOT_CONFIGURED", "OAuth provider 'google' is not configured.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record GoogleTokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("token_type") String tokenType,
            String scope
    ) {
    }

    private record GoogleUserInfoResponse(
            String id,
            String email,
            String name,
            String picture,
            @JsonProperty("verified_email") Boolean verifiedEmail
    ) {
    }
}
