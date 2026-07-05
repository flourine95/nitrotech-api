package com.nitrotech.api.infrastructure.auth.oauth.github;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "oauth.github")
public record GithubOAuthProperties(
        String clientId,
        String clientSecret,
        String redirectUri
) {
}
