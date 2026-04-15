package com.nitrotech.api.domain.cart.usecase;

import com.nitrotech.api.domain.cart.dto.CartItemData;
import com.nitrotech.api.domain.cart.repository.CartRepository;
import com.nitrotech.api.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UpdateCartItemUseCase {

    private final CartRepository cartRepository;

    public UpdateCartItemUseCase(CartRepository cartRepository) {
        this.cartRepository = cartRepository;
    }

    public CartItemData execute(Long userId, Long variantId, int quantity) {
        if (!cartRepository.hasItem(userId, variantId)) {
            throw new NotFoundException("CART_ITEM_NOT_FOUND", "Item not found in cart");
        }
        return cartRepository.updateItemQuantity(userId, variantId, quantity);
    }
}
