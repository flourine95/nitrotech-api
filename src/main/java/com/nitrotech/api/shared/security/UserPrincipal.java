package com.nitrotech.api.shared.security;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Stream;

@NullMarked
public record UserPrincipal(Long id, String email, String name, Set<String> roles, Set<String> permissions) implements UserDetails {

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
        return Stream.concat(
                        roles.stream().map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role),
                        permissions.stream()
                )
                .map(SimpleGrantedAuthority::new)
                .toList();
    }
}
