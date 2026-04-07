package com.nitrotech.api.domain.product.dto;

public record ProductFilter(
        String search,
        Boolean active,
        Boolean deleted,
        Long categoryId,
        Long brandId
) {}
