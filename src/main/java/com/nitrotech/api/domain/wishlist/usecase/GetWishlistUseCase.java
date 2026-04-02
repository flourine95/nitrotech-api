package com.nitrotech.api.domain.wishlist.usecase;

import com.nitrotech.api.domain.wishlist.dto.WishlistItemData;
import com.nitrotech.api.domain.wishlist.repository.WishlistRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GetWishlistUseCase {

    private final WishlistRepository wishlistRepository;

    public GetWishlistUseCase(WishlistRepository wishlistRepository) {
        this.wishlistRepository = wishlistRepository;
    }

    public List<WishlistItemData> execute(Long userId) {
        return wishlistRepository.findByUserId(userId);
    }
}
