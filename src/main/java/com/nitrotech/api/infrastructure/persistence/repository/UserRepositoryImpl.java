package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.domain.auth.dto.AuthResult;
import com.nitrotech.api.domain.auth.repository.UserRepository;
import com.nitrotech.api.infrastructure.persistence.entity.UserEntity;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository jpa;

    public UserRepositoryImpl(UserJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpa.existsByEmail(email);
    }

    @Override
    public AuthResult.UserData save(String name, String email, String hashedPassword) {
        UserEntity entity = new UserEntity();
        entity.setName(name);
        entity.setEmail(email);
        entity.setPassword(hashedPassword);
        UserEntity saved = jpa.save(entity);
        return new AuthResult.UserData(saved.getId(), saved.getName(), saved.getEmail());
    }

    @Override
    public Optional<UserCredential> findCredentialByEmail(String email) {
        return jpa.findByEmail(email)
                .map(e -> new UserCredential(e.getId(), e.getName(), e.getEmail(), e.getPassword()));
    }
}
