package com.nitrotech.api.domain.product.dto;

import java.util.List;

public record ProductFacets(
        List<CategoryFacet> categories,
        List<BrandFacet> brands,
        List<PriceRangeFacet> priceRanges
) {}
