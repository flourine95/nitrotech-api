package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.infrastructure.persistence.entity.WishlistEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WishlistJpaRepository extends JpaRepository<WishlistEntity, WishlistEntity.WishlistId> {

    @Query("SELECT w FROM WishlistEntity w WHERE w.userId = :userId ORDER BY w.createdAt DESC")
    List<WishlistEntity> findByUserId(@Param("userId") Long userId);

    boolean existsByUserIdAndProductId(Long userId, Long productId);

    void deleteByUserIdAndProductId(Long userId, Long productId);
}
