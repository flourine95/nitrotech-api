package com.nitrotech.api.application.auth.service;

import com.nitrotech.api.shared.exception.BadRequestException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;

@Service
public class OAuthStateService {

    private static final String SESSION_KEY_PREFIX = "oauth_state_";
    private final SecureRandom secureRandom = new SecureRandom();

    public String createState(String provider, HttpServletRequest request) {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        request.getSession(true).setAttribute(sessionKey(provider), state);
        return state;
    }

    public void validateState(String provider, String state, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        String key = sessionKey(provider);
        Object expected = session == null ? null : session.getAttribute(key);
        if (session != null) {
            session.removeAttribute(key);
        }
        if (!(expected instanceof String expectedState) || state == null || !expectedState.equals(state)) {
            throw new BadRequestException("OAUTH_STATE_MISMATCH", "OAuth state is invalid or expired");
        }
    }

    private String sessionKey(String provider) {
        return SESSION_KEY_PREFIX + provider.toLowerCase(Locale.ROOT);
    }
}
