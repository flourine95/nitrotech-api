package com.nitrotech.api.domain.brand.dto;

public record UpdateBrandCommand(
        Long id,
        String name,
        String slug,
        String logo,
        String description,
        Boolean active
) {}
