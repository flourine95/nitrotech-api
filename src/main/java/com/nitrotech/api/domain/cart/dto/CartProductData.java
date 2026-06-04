package com.nitrotech.api.domain.cart.dto;

public record CartProductData(
        Long id,
        String name,
        String slug,
        String thumbnail
) {}
