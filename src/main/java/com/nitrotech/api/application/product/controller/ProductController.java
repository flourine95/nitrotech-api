package com.nitrotech.api.application.product.controller;

import com.nitrotech.api.application.product.request.*;
import com.nitrotech.api.domain.product.dto.*;
import com.nitrotech.api.domain.product.usecase.*;
import com.nitrotech.api.shared.request.PaginationRequest;
import com.nitrotech.api.shared.response.ApiResult;
import com.nitrotech.api.shared.util.SortUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private static final Set<String> SORTABLE_FIELDS =
            Set.of("id", "name", "slug", "active", "createdAt", "updatedAt");

    private final GetProductsUseCase getProductsUseCase;
    private final GetProductUseCase getProductUseCase;
    private final CreateProductUseCase createProductUseCase;
    private final UpdateProductUseCase updateProductUseCase;
    private final DeleteProductUseCase deleteProductUseCase;
    private final RestoreProductUseCase restoreProductUseCase;
    private final HardDeleteProductUseCase hardDeleteProductUseCase;
    private final CreateVariantUseCase createVariantUseCase;
    private final UpdateVariantUseCase updateVariantUseCase;
    private final DeleteVariantUseCase deleteVariantUseCase;
    private final SearchProductsUseCase searchProductsUseCase;
    private final GetProductFacetsUseCase getProductFacetsUseCase;

    @GetMapping("/facets")
    public ResponseEntity<ApiResult<ProductFacets>> getFacets(
            @Valid @ModelAttribute ProductFacetsRequest request
    ) {
        ProductFilter filter = new ProductFilter(
                request.getSearch(),
                request.getActive(),
                null,
                request.getCategory(),
                request.getBrand(),
                request.getMinPrice(),
                request.getMaxPrice()
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
            @Valid @ModelAttribute ProductListRequest filter,
            @Valid @ModelAttribute PaginationRequest pagination
    ) {
        Pageable pageable = SortUtils.toPageable(
                pagination.getPage(),
                pagination.getSize(),
                pagination.getSort(),
                SORTABLE_FIELDS,
                "createdAt"
        );
        return ResponseEntity.ok(ApiResult.paged(
                getProductsUseCase.execute(filter.toFilter(), pageable)
        ));
    }

    @GetMapping("/{idOrSlug}")
    public ResponseEntity<ApiResult<ProductData>> get(
            @PathVariable String idOrSlug
    ) {
        return ResponseEntity.ok(ApiResult.ok(getProductUseCase.execute(idOrSlug)));
    }

    @PostMapping
    public ResponseEntity<ApiResult<ProductData>> create(@Valid @RequestBody CreateProductRequest req) {
        List<CreateVariantCommand> variants = req.variants() == null ? null :
                req.variants().stream().map(v -> new CreateVariantCommand(
                        v.sku(), v.name(), v.price(), v.attributes(), v.active(), v.imageId())).toList();
        ProductData data = createProductUseCase.execute(new CreateProductCommand(
                req.categoryId(), req.brandId(), req.name(), req.slug(),
                req.description(), req.thumbnail(), req.specs(), req.active(),
                req.images(), variants, req.manualBadge(), req.manualBadgeExpiresAt()));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.created(data));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResult<ProductData>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductRequest req
    ) {
        ProductData data = updateProductUseCase.execute(new UpdateProductCommand(
                id, req.categoryId(), req.brandId(), req.name(), req.slug(),
                req.description(), req.thumbnail(), req.specs(), req.active(), req.images(),
                req.manualBadge(), req.manualBadgeExpiresAt()));
        return ResponseEntity.ok(ApiResult.ok(data));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResult<Void>> delete(
            @PathVariable Long id
    ) {
        deleteProductUseCase.execute(id);
        return ResponseEntity.ok(ApiResult.ok("Product deleted successfully"));
    }

    @PatchMapping("/{id}/restore")
    public ResponseEntity<ApiResult<Void>> restore(
            @PathVariable Long id
    ) {
        restoreProductUseCase.execute(id);
        return ResponseEntity.ok(ApiResult.ok("Product restored successfully"));
    }

    @DeleteMapping("/{id}/permanent")
    public ResponseEntity<ApiResult<Void>> hardDelete(
            @PathVariable Long id
    ) {
        hardDeleteProductUseCase.execute(id);
        return ResponseEntity.ok(ApiResult.ok("Product permanently deleted"));
    }

    @PostMapping("/{productId}/variants")
    public ResponseEntity<ApiResult<ProductVariantData>> createVariant(
            @PathVariable Long productId,
            @Valid @RequestBody CreateVariantRequest req
    ) {
        ProductVariantData data = createVariantUseCase.execute(productId,
                new CreateVariantCommand(req.sku(), req.name(), req.price(), req.attributes(), req.active(), req.imageId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.created(data));
    }

    @PutMapping("/{productId}/variants/{variantId}")
    public ResponseEntity<ApiResult<ProductVariantData>> updateVariant(
            @PathVariable Long productId,
            @PathVariable Long variantId,
            @Valid @RequestBody UpdateVariantRequest req
    ) {
        ProductVariantData data = updateVariantUseCase.execute(new UpdateVariantCommand(
                variantId, productId, req.sku(), req.name(), req.price(), req.attributes(), req.active(), req.imageId()));
        return ResponseEntity.ok(ApiResult.ok(data));
    }

    @DeleteMapping("/{productId}/variants/{variantId}")
    public ResponseEntity<ApiResult<Void>> deleteVariant(
            @PathVariable Long productId,
            @PathVariable Long variantId
    ) {
        deleteVariantUseCase.execute(variantId);
        return ResponseEntity.ok(ApiResult.ok("Variant deleted successfully"));
    }
}