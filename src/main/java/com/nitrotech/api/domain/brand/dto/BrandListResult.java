package com.nitrotech.api.domain.brand.dto;

import org.springframework.data.domain.Page;

public record BrandListResult(
        Page<BrandData> page,
        BrandFacets facets
) {}
