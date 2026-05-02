package com.nitrotech.api.domain.category.dto;

import java.util.List;

public record CategoryTreeResult(
        List<CategoryData> tree,
        CategoryFacets facets
) {
}
