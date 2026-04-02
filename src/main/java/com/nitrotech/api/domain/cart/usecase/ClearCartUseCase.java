package com.nitrotech.api.domain.cart.usecase;

import com.nitrotech.api.domain.cart.repository.CartRepository;
import org.springframework.stereotype.Service;

@Service
public class ClearCartUseCase {

    private final CartRepository cartRepository;

    public ClearCartUseCase(CartRepository cartRepository) {
        this.cartRepository = cartRepository;
    }

    public void execute(Long userId) {
        cartRepository.clearCart(userId);
    }
}
