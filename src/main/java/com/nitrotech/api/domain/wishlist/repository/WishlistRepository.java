package com.nitrotech.api.domain.wishlist.repository;

import com.nitrotech.api.domain.wishlist.dto.WishlistItemData;

import java.util.List;

public interface WishlistRepository {
    void add(Long userId, Long productId);
    void remove(Long userId, Long productId);
    List<WishlistItemData> findByUserId(Long userId);
    boolean exists(Long userId, Long productId);
}
