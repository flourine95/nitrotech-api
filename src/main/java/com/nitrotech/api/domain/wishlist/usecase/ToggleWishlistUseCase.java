package com.nitrotech.api.domain.wishlist.usecase;

import com.nitrotech.api.domain.product.exception.ProductNotFoundException;
import com.nitrotech.api.domain.product.repository.ProductRepository;
import com.nitrotech.api.domain.wishlist.repository.WishlistRepository;
import org.springframework.stereotype.Service;

@Service
public class ToggleWishlistUseCase {

    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;

    public ToggleWishlistUseCase(WishlistRepository wishlistRepository, ProductRepository productRepository) {
        this.wishlistRepository = wishlistRepository;
        this.productRepository = productRepository;
    }

    // Trả về true nếu đã thêm, false nếu đã xóa
    public boolean execute(Long userId, Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ProductNotFoundException();
        }
        if (wishlistRepository.exists(userId, productId)) {
            wishlistRepository.remove(userId, productId);
            return false;
        }
        wishlistRepository.add(userId, productId);
        return true;
    }
}
