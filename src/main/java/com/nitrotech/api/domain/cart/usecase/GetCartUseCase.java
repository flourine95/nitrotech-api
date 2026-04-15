package com.nitrotech.api.domain.cart.usecase;

import com.nitrotech.api.domain.cart.dto.CartData;
import com.nitrotech.api.domain.cart.repository.CartRepository;
import org.springframework.stereotype.Service;

@Service
public class GetCartUseCase {

    private final CartRepository cartRepository;

    public GetCartUseCase(CartRepository cartRepository) {
        this.cartRepository = cartRepository;
    }

    public CartData execute(Long userId) {
        return cartRepository.getOrCreateCart(userId);
    }
}
