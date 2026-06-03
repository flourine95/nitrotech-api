package com.nitrotech.api.application.product.controller;

import com.nitrotech.api.application.product.request.*;
import com.nitrotech.api.domain.product.dto.*;
import com.nitrotech.api.domain.product.usecase.*;
import com.nitrotech.api.shared.response.ApiResult;
import com.nitrotech.api.shared.validation.ValidSortFields;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Validated
public class ProductController {

    private final GetProductsUseCase getProductsUseCase;
    private final GetPublicProductUseCase getPublicProductUseCase;
    private final SearchProductsUseCase searchProductsUseCase;
    private final GetProductFacetsUseCase getProductFacetsUseCase;
    private final GetRelatedProductsUseCase getRelatedProductsUseCase;

    @GetMapping("/facets")
    public ResponseEntity<ApiResult<ProductFacets>> getFacets(
            @Valid @ModelAttribute ProductFacetsRequest request
    ) {
        ProductFilter filter = new ProductFilter(
                request.getSearch(),
                true,
                false,
                request.getCategory(),
                request.getBrand(),
                request.getMinPrice(),
                request.getMaxPrice(),
                request.getBadge(),
                true
        );
        ProductFacets facets = getProductFacetsUseCase.execute(filter);
        return ResponseEntity.ok(ApiResult.ok(facets));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResult<List<ProductPickerItem>>> search(
            @Valid @ModelAttribute ProductSearchRequest request
    ) {
        List<ProductPickerItem> results = searchProductsUseCase.execute(
                request.getSearch(),
                request.getCategory(),
                request.getBrand(),
                request.getExcludeId(),
                request.getLimit()
        );
        return ResponseEntity.ok(ApiResult.ok(results));
    }

    @GetMapping
    public ResponseEntity<ApiResult<List<ProductData>>> list(
            @RequestParam(name = "search", required = false) @Size(max = 100, message = "Search query must not exceed 100 characters") String search,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "brand", required = false) List<String> brand,
            @RequestParam(name = "badge", required = false) String badge,
            @RequestParam(name = "minPrice", required = false) @PositiveOrZero(message = "Min price must be greater than or equal to 0") BigDecimal minPrice,
            @RequestParam(name = "maxPrice", required = false) @PositiveOrZero(message = "Max price must be greater than or equal to 0") BigDecimal maxPrice,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            @ValidSortFields({"id", "name", "slug", "active", "price", "createdAt", "updatedAt"})
            Pageable pageable
    ) {
        ProductFilter filter = new ProductFilter(search, true, false, category, brand, minPrice, maxPrice, badge, true);
        return ResponseEntity.ok(ApiResult.paged(
                getProductsUseCase.execute(filter, pageable)
        ));
    }

    @GetMapping("/{id}/related")
    public ResponseEntity<ApiResult<List<ProductData>>> related(
            @PathVariable Long id,
            @RequestParam(name = "limit", defaultValue = "4") @Min(1) @Max(12) int limit
    ) {
        return ResponseEntity.ok(ApiResult.ok(getRelatedProductsUseCase.execute(id, limit)));
    }

    @GetMapping("/{idOrSlug}")
    public ResponseEntity<ApiResult<ProductData>> get(
            @PathVariable String idOrSlug
    ) {
        return ResponseEntity.ok(ApiResult.ok(getPublicProductUseCase.execute(idOrSlug)));
    }
}
