package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.domain.wishlist.dto.WishlistItemData;
import com.nitrotech.api.domain.wishlist.repository.WishlistRepository;
import com.nitrotech.api.infrastructure.persistence.entity.ProductEntity;
import com.nitrotech.api.infrastructure.persistence.entity.WishlistEntity;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public class WishlistRepositoryImpl implements WishlistRepository {

    private final WishlistJpaRepository jpa;
    private final ProductJpaRepository productJpa;

    public WishlistRepositoryImpl(WishlistJpaRepository jpa, ProductJpaRepository productJpa) {
        this.jpa = jpa;
        this.productJpa = productJpa;
    }

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
        return jpa.findByUserId(userId).stream().map(w -> {
            ProductEntity product = productJpa.findById(w.getProductId()).orElse(null);
            return new WishlistItemData(
                    w.getProductId(),
                    product != null ? product.getName() : null,
                    product != null ? product.getSlug() : null,
                    product != null ? product.getThumbnail() : null,
                    w.getCreatedAt()
            );
        }).toList();
    }

    @Override
    public boolean exists(Long userId, Long productId) {
        return jpa.existsByUserIdAndProductId(userId, productId);
    }
}
