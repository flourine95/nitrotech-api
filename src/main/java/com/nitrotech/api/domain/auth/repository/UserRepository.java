package com.nitrotech.api.domain.auth.repository;

import com.nitrotech.api.domain.auth.dto.AuthResult;

import java.util.Optional;

public interface UserRepository {
    boolean existsByEmail(String email);
    AuthResult.UserData save(String name, String email, String hashedPassword);
    Optional<UserCredential> findCredentialByEmail(String email);

    record UserCredential(Long id, String name, String email, String hashedPassword) {}
}
