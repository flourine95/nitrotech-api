package com.nitrotech.api.domain.cart.usecase;

import com.nitrotech.api.domain.cart.dto.CartItemData;
import com.nitrotech.api.domain.cart.repository.CartRepository;
import com.nitrotech.api.domain.inventory.repository.InventoryRepository;
import com.nitrotech.api.domain.product.repository.ProductRepository;
import com.nitrotech.api.shared.exception.DomainException;
import com.nitrotech.api.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AddToCartUseCase {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;

    public AddToCartUseCase(CartRepository cartRepository, ProductRepository productRepository,
                             InventoryRepository inventoryRepository) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
    }

    public CartItemData execute(Long userId, Long variantId, int quantity) {
        if (!productRepository.existsVariantById(variantId)) {
            throw new NotFoundException("VARIANT_NOT_FOUND", "Variant not found");
        }
        if (!inventoryRepository.hasSufficientStock(variantId, quantity)) {
            int available = inventoryRepository.getQuantity(variantId);
            throw new DomainException("INSUFFICIENT_STOCK",
                    "Insufficient stock. Available: " + available) {};
        }
        if (cartRepository.hasItem(userId, variantId)) {
            return cartRepository.updateItemQuantity(userId, variantId, quantity);
        }
        return cartRepository.addItem(userId, variantId, quantity);
    }
}
