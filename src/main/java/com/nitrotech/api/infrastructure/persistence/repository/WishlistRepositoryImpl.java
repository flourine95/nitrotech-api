package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.domain.wishlist.dto.WishlistItemData;
import com.nitrotech.api.domain.wishlist.repository.WishlistRepository;
import com.nitrotech.api.infrastructure.persistence.entity.ProductEntity;
import com.nitrotech.api.infrastructure.persistence.entity.WishlistEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class WishlistRepositoryImpl implements WishlistRepository {

    private final WishlistJpaRepository jpa;

    @Override
    public void add(Long userId, Long productId) {
        WishlistEntity entity = new WishlistEntity();
        entity.setUserId(userId);
        entity.setProductId(productId);
        jpa.save(entity);
    }

    @Override
    @Transactional
    public void remove(Long userId, Long productId) {
        jpa.deleteByUserIdAndProductId(userId, productId);
    }

    @Override
    public List<WishlistItemData> findByUserId(Long userId) {
        return jpa.findByUserId(userId).stream()
                .map(this::toWishlistItemData)
                .toList();
    }
    
    private WishlistItemData toWishlistItemData(WishlistEntity entity) {
        ProductEntity product = entity.getProduct();
        return new WishlistItemData(
                entity.getProductId(),
                product != null ? product.getName() : null,
                product != null ? product.getSlug() : null,
                product != null ? product.getThumbnail() : null,
                entity.getCreatedAt()
        );
    }

    @Override
    public boolean exists(Long userId, Long productId) {
        return jpa.existsByUserIdAndProductId(userId, productId);
    }
}
