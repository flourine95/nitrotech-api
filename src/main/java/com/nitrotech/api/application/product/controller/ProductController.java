package com.nitrotech.api.application.product.controller;

import com.nitrotech.api.application.product.request.*;
import com.nitrotech.api.domain.product.dto.*;
import com.nitrotech.api.domain.product.usecase.*;
import com.nitrotech.api.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final GetProductsUseCase getProductsUseCase;
    private final GetProductUseCase getProductUseCase;
    private final CreateProductUseCase createProductUseCase;
    private final UpdateProductUseCase updateProductUseCase;
    private final DeleteProductUseCase deleteProductUseCase;
    private final CreateVariantUseCase createVariantUseCase;
    private final UpdateVariantUseCase updateVariantUseCase;
    private final DeleteVariantUseCase deleteVariantUseCase;

    public ProductController(GetProductsUseCase getProductsUseCase, GetProductUseCase getProductUseCase,
                              CreateProductUseCase createProductUseCase, UpdateProductUseCase updateProductUseCase,
                              DeleteProductUseCase deleteProductUseCase, CreateVariantUseCase createVariantUseCase,
                              UpdateVariantUseCase updateVariantUseCase, DeleteVariantUseCase deleteVariantUseCase) {
        this.getProductsUseCase = getProductsUseCase;
        this.getProductUseCase = getProductUseCase;
        this.createProductUseCase = createProductUseCase;
        this.updateProductUseCase = updateProductUseCase;
        this.deleteProductUseCase = deleteProductUseCase;
        this.createVariantUseCase = createVariantUseCase;
        this.updateVariantUseCase = updateVariantUseCase;
        this.deleteVariantUseCase = deleteVariantUseCase;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductData>>> list(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(getProductsUseCase.execute(
                new ProductListQuery(categoryId, brandId, active, search, page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductData>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(getProductUseCase.execute(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductData>> create(@Valid @RequestBody CreateProductRequest req) {
        List<CreateVariantCommand> variants = req.variants() == null ? null :
                req.variants().stream().map(v -> new CreateVariantCommand(
                        v.sku(), v.name(), v.price(), v.attributes(), v.active())).toList();
        ProductData data = createProductUseCase.execute(new CreateProductCommand(
                req.categoryId(), req.brandId(), req.name(), req.slug(),
                req.description(), req.thumbnail(), req.specs(), req.active(),
                req.images(), variants));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(data));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductData>> update(
            @PathVariable Long id, @Valid @RequestBody UpdateProductRequest req) {
        ProductData data = updateProductUseCase.execute(new UpdateProductCommand(
                id, req.categoryId(), req.brandId(), req.name(), req.slug(),
                req.description(), req.thumbnail(), req.specs(), req.active(), req.images()));
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        deleteProductUseCase.execute(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Product deleted successfully"));
    }

    // ── variants ──────────────────────────────────────────────────────────────

    @PostMapping("/{productId}/variants")
    public ResponseEntity<ApiResponse<ProductVariantData>> createVariant(
            @PathVariable Long productId, @Valid @RequestBody CreateVariantRequest req) {
        ProductVariantData data = createVariantUseCase.execute(productId,
                new CreateVariantCommand(req.sku(), req.name(), req.price(), req.attributes(), req.active()));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(data));
    }

    @PutMapping("/{productId}/variants/{variantId}")
    public ResponseEntity<ApiResponse<ProductVariantData>> updateVariant(
            @PathVariable Long productId, @PathVariable Long variantId,
            @Valid @RequestBody UpdateVariantRequest req) {
        ProductVariantData data = updateVariantUseCase.execute(new UpdateVariantCommand(
                variantId, productId, req.sku(), req.name(), req.price(), req.attributes(), req.active()));
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @DeleteMapping("/{productId}/variants/{variantId}")
    public ResponseEntity<ApiResponse<Void>> deleteVariant(
            @PathVariable Long productId, @PathVariable Long variantId) {
        deleteVariantUseCase.execute(variantId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Variant deleted successfully"));
    }
}
