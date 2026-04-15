package com.nitrotech.api.domain.category.dto;

public record CreateCategoryCommand(
        String name,
        String slug,
        String description,
        String image,
        Long parentId,
        boolean active
) {}
