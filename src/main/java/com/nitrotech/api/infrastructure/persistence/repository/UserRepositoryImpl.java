package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.domain.auth.dto.AuthResult;
import com.nitrotech.api.domain.auth.dto.UserProfileData;
import com.nitrotech.api.domain.auth.repository.UserRepository;
import com.nitrotech.api.infrastructure.persistence.entity.UserEntity;
import com.nitrotech.api.shared.exception.NotFoundException;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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
        // status defaults to inactive — activated after email verification
        UserEntity saved = jpa.save(entity);
        return new AuthResult.UserData(saved.getId(), saved.getName(), saved.getEmail());
    }

    @Override
    public Optional<UserCredential> findCredentialByEmail(String email) {
        return jpa.findByEmail(email)
                .map(e -> new UserCredential(
                        e.getId(), e.getName(), e.getEmail(),
                        e.getPassword(), e.getStatus().name()
                ));
    }

    @Override
    public Optional<UserProfileData> findById(Long id) {
        return jpa.findById(id).map(this::toProfileData);
    }

    @Override
    public Optional<UserProfileData> findByEmail(String email) {
        return jpa.findByEmail(email).map(this::toProfileData);
    }

    @Override
    public UserProfileData updateProfile(Long id, String name, String phone, String avatar) {
        UserEntity entity = jpa.findById(id)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
        if (name != null) entity.setName(name);
        if (phone != null) entity.setPhone(phone);
        if (avatar != null) entity.setAvatar(avatar);
        entity.setUpdatedAt(LocalDateTime.now());
        return toProfileData(jpa.save(entity));
    }

    @Override
    public void updatePassword(Long id, String hashedPassword) {
        UserEntity entity = jpa.findById(id)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
        entity.setPassword(hashedPassword);
        entity.setUpdatedAt(LocalDateTime.now());
        jpa.save(entity);
    }

    @Override
    public void activateUser(Long id) {
        UserEntity entity = jpa.findById(id)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
        entity.setStatus(UserEntity.Status.active);
        entity.setUpdatedAt(LocalDateTime.now());
        jpa.save(entity);
    }

    private UserProfileData toProfileData(UserEntity e) {
        return new UserProfileData(
                e.getId(), e.getName(), e.getEmail(),
                e.getPhone(), e.getAvatar(),
                e.getStatus().name(), e.getProvider().name()
        );
    }
}
