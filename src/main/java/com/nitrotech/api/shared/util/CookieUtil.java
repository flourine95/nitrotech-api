package com.nitrotech.api.shared.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;


@Component
public class CookieUtil {

    public static final String REFRESH_TOKEN_COOKIE = "refreshToken";

    @Value("${app.cookie.secure:true}")
    private boolean secure;

    @Value("${app.cookie.same-site:Lax}")
    private String sameSite;

    @Value("${app.cookie.domain:}")
    private String domain;

    public void setRefreshTokenCookie(HttpServletResponse response, String token, int maxAgeSeconds) {
        // Dùng header thủ công để có SameSite (Cookie API không hỗ trợ trực tiếp)
        response.addHeader("Set-Cookie",
                buildSetCookieHeader(REFRESH_TOKEN_COOKIE, token, maxAgeSeconds));
    }

    public void clearRefreshTokenCookie(HttpServletResponse response) {
        response.addHeader("Set-Cookie",
                buildSetCookieHeader(REFRESH_TOKEN_COOKIE, "", 0));
    }

    public Optional<String> extractRefreshToken(HttpServletRequest request) {
        if (request.getCookies() == null) return Optional.empty();
        return Arrays.stream(request.getCookies())
                .filter(c -> REFRESH_TOKEN_COOKIE.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }

    private String buildSetCookieHeader(String name, String value, int maxAge) {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("=").append(value);
        sb.append("; Path=/api/auth");
        sb.append("; Max-Age=").append(maxAge);
        sb.append("; HttpOnly");
        if (secure) sb.append("; Secure");
        sb.append("; SameSite=").append(sameSite);
        if (!domain.isBlank()) sb.append("; Domain=").append(domain);
        return sb.toString();
    }
}
