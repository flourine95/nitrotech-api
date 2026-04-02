package com.nitrotech.api.domain.brand.dto;

public record CreateBrandCommand(
        String name,
        String slug,
        String logo,
        String description,
        boolean active
) {}
