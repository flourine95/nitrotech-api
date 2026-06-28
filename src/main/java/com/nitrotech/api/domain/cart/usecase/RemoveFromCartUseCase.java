package com.nitrotech.api.domain.cart.usecase;

import com.nitrotech.api.domain.cart.exception.CartItemNotFoundException;
import com.nitrotech.api.domain.cart.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RemoveFromCartUseCase {

    private final CartRepository cartRepository;

    public void execute(Long userId, Long variantId) {
        if (!cartRepository.hasItem(userId, variantId)) {
            throw new CartItemNotFoundException();
        }
        cartRepository.removeItem(userId, variantId);
    }
}
