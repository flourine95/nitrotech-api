package com.nitrotech.api.domain.category.dto;

public record UpdateCategoryCommand(
        Long id,
        String name,
        String slug,
        String description,
        String image,
        Long parentId,
        Boolean active
) {}
