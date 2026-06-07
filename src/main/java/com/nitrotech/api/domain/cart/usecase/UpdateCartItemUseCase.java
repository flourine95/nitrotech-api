package com.nitrotech.api.domain.cart.usecase;

import com.nitrotech.api.domain.cart.dto.CartItemData;
import com.nitrotech.api.domain.cart.repository.CartRepository;
import com.nitrotech.api.domain.inventory.repository.InventoryRepository;
import com.nitrotech.api.shared.exception.DomainException;
import com.nitrotech.api.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UpdateCartItemUseCase {

    private final CartRepository cartRepository;
    private final InventoryRepository inventoryRepository;

    public CartItemData execute(Long userId, Long variantId, int quantity) {
        if (!cartRepository.hasItem(userId, variantId)) {
            throw new NotFoundException("CART_ITEM_NOT_FOUND", "Item not found in cart");
        }
        if (!inventoryRepository.hasSufficientStock(variantId, quantity)) {
            int available = inventoryRepository.getQuantity(variantId);
            throw new DomainException("INSUFFICIENT_STOCK",
                    "Insufficient stock. Available: " + available) {};
        }
        return cartRepository.updateItemQuantity(userId, variantId, quantity);
    }
}
