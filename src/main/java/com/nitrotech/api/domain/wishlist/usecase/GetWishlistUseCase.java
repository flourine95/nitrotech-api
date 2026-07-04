package com.nitrotech.api.domain.wishlist.usecase;

import com.nitrotech.api.domain.wishlist.dto.WishlistItemData;
import com.nitrotech.api.domain.wishlist.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetWishlistUseCase {

    private final WishlistRepository wishlistRepository;

    public List<WishlistItemData> execute(Long userId) {
        return wishlistRepository.findByUserId(userId);
    }
}
