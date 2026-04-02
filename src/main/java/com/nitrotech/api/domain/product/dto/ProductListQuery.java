package com.nitrotech.api.domain.product.dto;

public record ProductListQuery(
        Long categoryId,
        Long brandId,
        Boolean active,
        String search,
        int page,
        int size
) {}
