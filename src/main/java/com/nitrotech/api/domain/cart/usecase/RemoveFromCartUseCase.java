package com.nitrotech.api.domain.cart.usecase;

import com.nitrotech.api.domain.cart.repository.CartRepository;
import com.nitrotech.api.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RemoveFromCartUseCase {

    private final CartRepository cartRepository;

    public void execute(Long userId, Long variantId) {
        if (!cartRepository.hasItem(userId, variantId)) {
            throw new NotFoundException("CART_ITEM_NOT_FOUND", "Item not found in cart");
        }
        cartRepository.removeItem(userId, variantId);
    }
}
