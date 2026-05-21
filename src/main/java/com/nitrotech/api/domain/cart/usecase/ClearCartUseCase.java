package com.nitrotech.api.domain.cart.usecase;

import com.nitrotech.api.domain.cart.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClearCartUseCase {

    private final CartRepository cartRepository;

    public void execute(Long userId) {
        cartRepository.clearCart(userId);
    }
}
