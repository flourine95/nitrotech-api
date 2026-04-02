package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.domain.cart.dto.CartData;
import com.nitrotech.api.domain.cart.dto.CartItemData;
import com.nitrotech.api.domain.cart.repository.CartRepository;
import com.nitrotech.api.infrastructure.persistence.entity.CartEntity;
import com.nitrotech.api.infrastructure.persistence.entity.CartItemEntity;
import com.nitrotech.api.infrastructure.persistence.entity.ProductVariantEntity;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class CartRepositoryImpl implements CartRepository {

    private final CartJpaRepository cartJpa;
    private final CartItemJpaRepository itemJpa;
    private final ProductVariantJpaRepository variantJpa;
    private final ProductJpaRepository productJpa;

    public CartRepositoryImpl(CartJpaRepository cartJpa, CartItemJpaRepository itemJpa,
                               ProductVariantJpaRepository variantJpa, ProductJpaRepository productJpa) {
        this.cartJpa = cartJpa;
        this.itemJpa = itemJpa;
        this.variantJpa = variantJpa;
        this.productJpa = productJpa;
    }

    @Override
    @Transactional
    public CartData getOrCreateCart(Long userId) {
        CartEntity cart = cartJpa.findByUserId(userId).orElseGet(() -> {
            CartEntity c = new CartEntity();
            c.setUserId(userId);
            return cartJpa.save(c);
        });
        return toCartData(cart);
    }

    @Override
    @Transactional
    public CartItemData addItem(Long userId, Long variantId, int quantity) {
        CartEntity cart = getOrCreateCartEntity(userId);
        CartItemEntity item = new CartItemEntity();
        item.setCartId(cart.getId());
        item.setVariantId(variantId);
        item.setQuantity(quantity);
        return toItemData(itemJpa.save(item));
    }

    @Override
    @Transactional
    public CartItemData updateItemQuantity(Long userId, Long variantId, int quantity) {
        CartEntity cart = getOrCreateCartEntity(userId);
        CartItemEntity item = itemJpa.findByCartIdAndVariantId(cart.getId(), variantId)
                .orElseThrow();
        item.setQuantity(item.getQuantity() + quantity);
        item.setUpdatedAt(LocalDateTime.now());
        return toItemData(itemJpa.save(item));
    }

    @Override
    @Transactional
    public void removeItem(Long userId, Long variantId) {
        CartEntity cart = getOrCreateCartEntity(userId);
        itemJpa.findByCartIdAndVariantId(cart.getId(), variantId)
                .ifPresent(itemJpa::delete);
    }

    @Override
    @Transactional
    public void clearCart(Long userId) {
        cartJpa.findByUserId(userId).ifPresent(cart -> itemJpa.deleteByCartId(cart.getId()));
    }

    @Override
    public boolean hasItem(Long userId, Long variantId) {
        return cartJpa.findByUserId(userId)
                .map(cart -> itemJpa.existsByCartIdAndVariantId(cart.getId(), variantId))
                .orElse(false);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private CartEntity getOrCreateCartEntity(Long userId) {
        return cartJpa.findByUserId(userId).orElseGet(() -> {
            CartEntity c = new CartEntity();
            c.setUserId(userId);
            return cartJpa.save(c);
        });
    }

    private CartData toCartData(CartEntity cart) {
        List<CartItemData> items = itemJpa.findByCartId(cart.getId())
                .stream().map(this::toItemData).toList();
        BigDecimal total = items.stream()
                .map(CartItemData::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartData(cart.getId(), cart.getUserId(), items, items.size(), total);
    }

    private CartItemData toItemData(CartItemEntity item) {
        ProductVariantEntity variant = variantJpa.findById(item.getVariantId()).orElseThrow();
        String productName = productJpa.findById(variant.getProductId())
                .map(p -> p.getName()).orElse(null);
        String thumbnail = productJpa.findById(variant.getProductId())
                .map(p -> p.getThumbnail()).orElse(null);
        BigDecimal subtotal = variant.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        return new CartItemData(
                item.getId(), variant.getId(), variant.getSku(), variant.getName(),
                variant.getPrice(), productName, thumbnail, item.getQuantity(), subtotal
        );
    }
}
