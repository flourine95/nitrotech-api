package com.nitrotech.api.domain.wishlist.dto;

import java.time.LocalDateTime;

public record WishlistItemData(
        Long productId,
        String productName,
        String productSlug,
        String productThumbnail,
        LocalDateTime addedAt
) {}
