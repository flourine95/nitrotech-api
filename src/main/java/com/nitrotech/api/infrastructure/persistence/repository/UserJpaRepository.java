package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.infrastructure.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {
    @Query("SELECT COUNT(u) > 0 FROM UserEntity u WHERE u.email = :email AND u.deletedAt IS NULL")
    boolean existsByEmail(@Param("email") String email);

    @Query("SELECT u FROM UserEntity u WHERE u.email = :email AND u.deletedAt IS NULL")
    Optional<UserEntity> findByEmail(@Param("email") String email);

    @Query("SELECT u FROM UserEntity u WHERE u.id IN :ids AND u.deletedAt IS NULL")
    List<UserEntity> findAllNotDeletedByIds(@Param("ids") List<Long> ids);

    @Modifying
    @Query("UPDATE UserEntity u SET u.deletedAt = :now WHERE u.id IN :ids AND u.deletedAt IS NULL")
    int bulkSoftDelete(@Param("ids") List<Long> ids, @Param("now") Instant now);

    @Modifying
    @Query("UPDATE UserEntity u SET u.status = :status WHERE u.id IN :ids AND u.deletedAt IS NULL")
    int bulkUpdateStatus(@Param("ids") List<Long> ids, @Param("status") UserEntity.Status status);

    @Query("SELECT u FROM UserEntity u WHERE u.id IN :ids AND u.deletedAt IS NOT NULL")
    List<UserEntity> findAllDeletedByIds(@Param("ids") List<Long> ids);

    @Modifying
    @Query("UPDATE UserEntity u SET u.deletedAt = null WHERE u.id IN :ids AND u.deletedAt IS NOT NULL")
    int bulkRestore(@Param("ids") List<Long> ids);
}
