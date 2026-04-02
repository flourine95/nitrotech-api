package com.nitrotech.api.domain.cart.usecase;

import com.nitrotech.api.domain.cart.dto.CartItemData;
import com.nitrotech.api.domain.cart.repository.CartRepository;
import com.nitrotech.api.domain.product.repository.ProductRepository;
import com.nitrotech.api.shared.exception.NotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AddToCartUseCase {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;

    public AddToCartUseCase(CartRepository cartRepository, ProductRepository productRepository) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
    }

    public CartItemData execute(Long userId, Long variantId, int quantity) {
        if (!productRepository.existsVariantById(variantId)) {
            throw new NotFoundException("VARIANT_NOT_FOUND", "Variant not found");
        }
        // Nếu đã có trong cart thì cộng thêm quantity
        if (cartRepository.hasItem(userId, variantId)) {
            return cartRepository.updateItemQuantity(userId, variantId, quantity);
        }
        return cartRepository.addItem(userId, variantId, quantity);
    }
}
