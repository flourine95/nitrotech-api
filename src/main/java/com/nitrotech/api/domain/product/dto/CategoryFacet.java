package com.nitrotech.api.domain.product.dto;

public record CategoryFacet(
        Long id,
        String name,
        String slug,
        Integer count
) {}
