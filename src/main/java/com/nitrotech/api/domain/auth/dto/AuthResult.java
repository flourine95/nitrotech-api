package com.nitrotech.api.domain.auth.dto;

import java.util.Set;

public record AuthResult(UserData user) {

    public static AuthResult ofUser(UserData user) {
        return new AuthResult(user);
    }

    public record UserData(Long id, String name, String email, Set<String> roles, Set<String> permissions) {
        public UserData(Long id, String name, String email) {
            this(id, name, email, Set.of(), Set.of());
        }
    }
}
