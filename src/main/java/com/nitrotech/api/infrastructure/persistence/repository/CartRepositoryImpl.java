package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.domain.cart.dto.*;
import com.nitrotech.api.domain.cart.repository.CartRepository;
import com.nitrotech.api.infrastructure.persistence.entity.CartEntity;
import com.nitrotech.api.infrastructure.persistence.entity.CartItemEntity;
import com.nitrotech.api.infrastructure.persistence.entity.InventoryEntity;
import com.nitrotech.api.infrastructure.persistence.entity.ProductEntity;
import com.nitrotech.api.infrastructure.persistence.entity.ProductVariantEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class CartRepositoryImpl implements CartRepository {

    private final CartJpaRepository cartJpa;
    private final CartItemJpaRepository itemJpa;
    private final ProductVariantJpaRepository variantJpa;
    private final ProductJpaRepository productJpa;
    private final InventoryJpaRepository inventoryJpa;
    private final ProductImageJpaRepository imageJpa;

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
        item.setQuantity(quantity);
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

    @Override
    public int getItemQuantity(Long userId, Long variantId) {
        return cartJpa.findByUserId(userId)
                .flatMap(cart -> itemJpa.findByCartIdAndVariantId(cart.getId(), variantId))
                .map(CartItemEntity::getQuantity)
                .orElse(0);
    }

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
        BigDecimal subtotal = items.stream()
                .map(CartItemData::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int totalItems = items.stream().mapToInt(CartItemData::quantity).sum();
        CartSummaryData summary = new CartSummaryData(
                totalItems,
                subtotal,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                subtotal
        );
        return new CartData(cart.getId(), cart.getUserId(), items, summary);
    }

    private CartItemData toItemData(CartItemEntity item) {
        ProductVariantEntity variant = variantJpa.findById(item.getVariantId()).orElseThrow();
        ProductEntity product = productJpa.findById(variant.getProductId()).orElseThrow();
        InventoryEntity inventory = inventoryJpa.findByVariantId(variant.getId()).orElse(null);
        String imageUrl = variant.getImageId() == null
                ? null
                : imageJpa.findById(variant.getImageId()).map(image -> image.getUrl()).orElse(null);
        BigDecimal subtotal = variant.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        Integer stockQuantity = inventory == null ? null : inventory.getQuantity();
        Integer lowStockThreshold = inventory == null ? null : inventory.getLowStockThreshold();
        Boolean inStock = stockQuantity == null ? null : stockQuantity > 0;
        Boolean lowStock = stockQuantity == null || lowStockThreshold == null
                ? null
                : stockQuantity <= lowStockThreshold;
        CartProductData productData = new CartProductData(
                product.getId(), product.getName(), product.getSlug(), product.getThumbnail()
        );
        CartVariantData variantData = new CartVariantData(
                variant.getId(),
                variant.getProductId(),
                variant.getSku(),
                variant.getName(),
                variant.getPrice(),
                variant.getAttributes(),
                variant.isActive(),
                variant.getImageId(),
                imageUrl,
                stockQuantity,
                lowStockThreshold,
                inStock,
                lowStock,
                productData
        );
        return new CartItemData(
                item.getId(),
                item.getCartId(),
                variant.getId(),
                variantData,
                item.getQuantity(),
                subtotal,
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }
}
