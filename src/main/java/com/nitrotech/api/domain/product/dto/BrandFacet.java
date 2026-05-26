package com.nitrotech.api.domain.product.dto;

public record BrandFacet(
        Long id,
        String name,
        String slug,
        Integer count
) {}
