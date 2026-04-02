package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.infrastructure.persistence.entity.CartItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CartItemJpaRepository extends JpaRepository<CartItemEntity, Long> {

    List<CartItemEntity> findByCartId(Long cartId);

    Optional<CartItemEntity> findByCartIdAndVariantId(Long cartId, Long variantId);

    boolean existsByCartIdAndVariantId(Long cartId, Long variantId);

    void deleteByCartId(Long cartId);

    @Query("SELECT ci FROM CartItemEntity ci WHERE ci.cartId = :cartId AND ci.variantId = :variantId")
    Optional<CartItemEntity> findItem(@Param("cartId") Long cartId, @Param("variantId") Long variantId);
}
