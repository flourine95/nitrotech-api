package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.domain.auth.dto.AuthResult;
import com.nitrotech.api.domain.auth.dto.UserProfileData;
import com.nitrotech.api.domain.auth.repository.UserRepository;
import com.nitrotech.api.infrastructure.persistence.entity.UserEntity;
import com.nitrotech.api.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository jpa;
    private final JdbcTemplate jdbc;

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
        assignRole(saved.getId(), "customer");
        UserAuthorities authorities = findAuthoritiesByUserId(saved.getId());
        return new AuthResult.UserData(saved.getId(), saved.getName(), saved.getEmail(),
                authorities.roles(), authorities.permissions());
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
    public UserAuthorities findAuthoritiesByUserId(Long id) {
        Set<String> roles = new LinkedHashSet<>(jdbc.queryForList("""
                SELECT r.slug
                FROM user_roles ur
                JOIN roles r ON r.id = ur.role_id
                WHERE ur.user_id = ?
                  AND r.active = TRUE
                  AND r.deleted_at IS NULL
                ORDER BY r.slug
                """, String.class, id));

        Set<String> permissions = new LinkedHashSet<>(jdbc.queryForList("""
                SELECT DISTINCT p.slug
                FROM user_roles ur
                JOIN roles r ON r.id = ur.role_id
                JOIN role_permissions rp ON rp.role_id = r.id
                JOIN permissions p ON p.id = rp.permission_id
                WHERE ur.user_id = ?
                  AND r.active = TRUE
                  AND r.deleted_at IS NULL
                  AND p.deleted_at IS NULL
                ORDER BY p.slug
                """, String.class, id));

        return new UserAuthorities(roles, permissions);
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
        return toProfileData(jpa.save(entity));
    }

    @Override
    public void updatePassword(Long id, String hashedPassword) {
        UserEntity entity = jpa.findById(id)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
        entity.setPassword(hashedPassword);
        jpa.save(entity);
    }

    @Override
    public void activateUser(Long id) {
        UserEntity entity = jpa.findById(id)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
        entity.setStatus(UserEntity.Status.active);
        jpa.save(entity);
    }

    private UserProfileData toProfileData(UserEntity e) {
        return new UserProfileData(
                e.getId(), e.getName(), e.getEmail(),
                e.getPhone(), e.getAvatar(),
                e.getStatus().name(), e.getProvider().name()
        );
    }

    private void assignRole(Long userId, String roleSlug) {
        jdbc.update("""
                INSERT INTO user_roles (user_id, role_id)
                SELECT ?, r.id
                FROM roles r
                WHERE r.slug = ?
                ON CONFLICT DO NOTHING
                """, userId, roleSlug);
    }
}
