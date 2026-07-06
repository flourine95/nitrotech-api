package com.nitrotech.api.domain.auth;

import java.util.Locale;

public enum UserStatus {
    inactive,
    active,
    banned,
    suspended;

    public String value() {
        return name();
    }

    public static UserStatus fromValue(String value) {
        return UserStatus.valueOf(value.trim().toLowerCase(Locale.ROOT));
    }
}
