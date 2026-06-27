package com.nitrotech.api.domain.auth.repository;

import com.nitrotech.api.domain.auth.dto.AuthResult;
import com.nitrotech.api.domain.auth.dto.UserProfileData;

import java.util.Optional;
import java.util.Set;

public interface UserRepository {
    boolean existsByEmail(String email);

    AuthResult.UserData save(String name, String email, String hashedPassword);

    Optional<UserCredential> findCredentialByEmail(String email);

    UserAuthorities findAuthoritiesByUserId(Long id);

    Optional<UserProfileData> findById(Long id);

    Optional<UserProfileData> findByEmail(String email);

    UserProfileData updateProfile(Long id, String name, String phone, String avatar);

    void updatePassword(Long id, String hashedPassword);

    void activateUser(Long id);

    record UserCredential(Long id, String name, String email, String hashedPassword, String status) {
    }

    record UserAuthorities(Set<String> roles, Set<String> permissions) {
    }
}
