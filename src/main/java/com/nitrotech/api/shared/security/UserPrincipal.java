package com.nitrotech.api.shared.security;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@NullMarked
public record UserPrincipal(Long id, String email, String name) implements UserDetails {

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public @Nullable String getPassword() {
        return null;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }
}
