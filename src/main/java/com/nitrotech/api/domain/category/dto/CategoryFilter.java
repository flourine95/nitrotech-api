package com.nitrotech.api.domain.category.dto;

public record CategoryFilter(
        String search,
        Boolean active,
        Boolean deleted,
        Long parentId
) {}
