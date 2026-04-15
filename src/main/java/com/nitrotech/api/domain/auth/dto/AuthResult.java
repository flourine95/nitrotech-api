package com.nitrotech.api.domain.auth.dto;

public record AuthResult(UserData user) {

    public static AuthResult ofUser(UserData user) {
        return new AuthResult(user);
    }

    public record UserData(Long id, String name, String email) {}
}
