package com.nitrotech.api.domain.cart.usecase;

import com.nitrotech.api.domain.cart.dto.CartItemData;
import com.nitrotech.api.domain.cart.exception.CartItemNotFoundException;
import com.nitrotech.api.domain.cart.repository.CartRepository;
import com.nitrotech.api.domain.inventory.exception.InsufficientStockException;
import com.nitrotech.api.domain.inventory.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UpdateCartItemUseCase {

    private final CartRepository cartRepository;
    private final InventoryRepository inventoryRepository;

    public CartItemData execute(Long userId, Long variantId, int quantity) {
        if (!cartRepository.hasItem(userId, variantId)) {
            throw new CartItemNotFoundException();
        }
        if (!inventoryRepository.hasSufficientStock(variantId, quantity)) {
            int available = inventoryRepository.getQuantity(variantId);
            throw new InsufficientStockException(available);
        }
        return cartRepository.updateItemQuantity(userId, variantId, quantity);
    }
}
