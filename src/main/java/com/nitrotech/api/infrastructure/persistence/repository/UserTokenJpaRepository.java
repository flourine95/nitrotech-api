package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.infrastructure.persistence.entity.UserTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserTokenJpaRepository extends JpaRepository<UserTokenEntity, Long> {
    Optional<UserTokenEntity> findByTokenAndType(String token, UserTokenEntity.Type type);
    void deleteByUserIdAndType(Long userId, UserTokenEntity.Type type);
}
