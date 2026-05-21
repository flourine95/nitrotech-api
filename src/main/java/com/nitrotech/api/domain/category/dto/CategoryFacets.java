package com.nitrotech.api.domain.category.dto;

public record CategoryFacets(
        long active,
        long inactive,
        long deleted,
        long root,
        long withChildren
) {}
