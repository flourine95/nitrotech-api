package com.nitrotech.api.domain.category.dto;

import org.springframework.data.domain.Page;

public record CategoryPageResult(
        Page<CategoryData> page,
        CategoryFacets facets
) {}
