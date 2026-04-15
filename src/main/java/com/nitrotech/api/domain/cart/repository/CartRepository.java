package com.nitrotech.api.domain.cart.repository;

import com.nitrotech.api.domain.cart.dto.CartData;
import com.nitrotech.api.domain.cart.dto.CartItemData;

public interface CartRepository {
    CartData getOrCreateCart(Long userId);
    CartItemData addItem(Long userId, Long variantId, int quantity);
    CartItemData updateItemQuantity(Long userId, Long variantId, int quantity);
    void removeItem(Long userId, Long variantId);
    void clearCart(Long userId);
    boolean hasItem(Long userId, Long variantId);
}
