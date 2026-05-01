package com.nitrotech.api.domain.category.dto;

public record BreadcrumbItem(
        Long id,
        String name,
        String slug,
        Boolean active
) {}
