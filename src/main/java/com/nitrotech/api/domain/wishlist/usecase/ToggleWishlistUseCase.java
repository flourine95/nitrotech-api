package com.nitrotech.api.domain.wishlist.usecase;

import com.nitrotech.api.domain.product.repository.ProductRepository;
import com.nitrotech.api.domain.wishlist.repository.WishlistRepository;
import com.nitrotech.api.shared.exception.NotFoundException;
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
            throw new NotFoundException("PRODUCT_NOT_FOUND", "Product not found");
        }
        if (wishlistRepository.exists(userId, productId)) {
            wishlistRepository.remove(userId, productId);
            return false;
        }
        wishlistRepository.add(userId, productId);
        return true;
    }
}
