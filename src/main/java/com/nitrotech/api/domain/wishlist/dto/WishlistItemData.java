package com.nitrotech.api.domain.wishlist.dto;

import java.time.Instant;

public record WishlistItemData(
        Long productId,
        String productName,
        String productSlug,
        String productThumbnail,
        Instant addedAt
) {}
