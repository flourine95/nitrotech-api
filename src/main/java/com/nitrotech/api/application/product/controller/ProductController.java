package com.nitrotech.api.application.product.controller;

import com.nitrotech.api.application.product.request.*;
import com.nitrotech.api.domain.product.dto.*;
import com.nitrotech.api.domain.product.usecase.*;
import com.nitrotech.api.shared.response.ApiResult;
import com.nitrotech.api.shared.util.SortUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Products", description = "Product and variant management APIs")
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

    @Operation(summary = "List products", description = "Get paginated list of products with optional filters by search, category, brand, and status.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Products retrieved successfully")
    })
    @GetMapping
    public ResponseEntity<ApiResult<List<ProductData>>> list(
            @Parameter(description = "Search by name or slug") @RequestParam(required = false) String search,
            @Parameter(description = "Filter by active status") @RequestParam(required = false) Boolean active,
            @Parameter(description = "Set to true to include soft-deleted products") @RequestParam(required = false) Boolean deleted,
            @Parameter(description = "Filter by category ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "Filter by brand ID") @RequestParam(required = false) Long brandId,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort fields, e.g. name,asc") @RequestParam(required = false) List<String> sort
    ) {
        Pageable pageable = SortUtils.toPageable(page, size, sort, SORTABLE_FIELDS, "createdAt");
        return ResponseEntity.ok(ApiResult.paged(
                getProductsUseCase.execute(new ProductFilter(search, active, deleted, categoryId, brandId), pageable)));
    }

    @Operation(summary = "Get product by ID", description = "Retrieve a single product with all its variants and images.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product found"),
            @ApiResponse(responseCode = "404", description = "Product not found", content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResult<ProductData>> get(
            @Parameter(description = "Product ID") @PathVariable Long id
    ) {
        return ResponseEntity.ok(ApiResult.ok(getProductUseCase.execute(id)));
    }

    @Operation(summary = "Create product", description = "Create a new product with optional variants.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Product created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input - validation error", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "409", description = "Product with this slug already exists", content = @Content(mediaType = "application/json"))
    })
    @PostMapping
    public ResponseEntity<ApiResult<ProductData>> create(@Valid @RequestBody CreateProductRequest req) {
        List<CreateVariantCommand> variants = req.variants() == null ? null :
                req.variants().stream().map(v -> new CreateVariantCommand(
                        v.sku(), v.name(), v.price(), v.attributes(), v.active())).toList();
        ProductData data = createProductUseCase.execute(new CreateProductCommand(
                req.categoryId(), req.brandId(), req.name(), req.slug(),
                req.description(), req.thumbnail(), req.specs(), req.active(),
                req.images(), variants));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.created(data));
    }

    @Operation(summary = "Update product", description = "Update an existing product's details. Does not affect variants.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input - validation error", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Product not found", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "409", description = "Slug already in use by another product", content = @Content(mediaType = "application/json"))
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResult<ProductData>> update(
            @Parameter(description = "Product ID") @PathVariable Long id,
            @Valid @RequestBody UpdateProductRequest req
    ) {
        ProductData data = updateProductUseCase.execute(new UpdateProductCommand(
                id, req.categoryId(), req.brandId(), req.name(), req.slug(),
                req.description(), req.thumbnail(), req.specs(), req.active(), req.images()));
        return ResponseEntity.ok(ApiResult.ok(data));
    }

    @Operation(summary = "Soft delete product", description = "Soft delete a product. Can be restored later.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Product not found", content = @Content(mediaType = "application/json"))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResult<Void>> delete(
            @Parameter(description = "Product ID") @PathVariable Long id
    ) {
        deleteProductUseCase.execute(id);
        return ResponseEntity.ok(ApiResult.ok(null, "Product deleted successfully"));
    }

    @Operation(summary = "Restore product", description = "Restore a previously soft-deleted product.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product restored successfully"),
            @ApiResponse(responseCode = "404", description = "Product not found", content = @Content(mediaType = "application/json"))
    })
    @PatchMapping("/{id}/restore")
    public ResponseEntity<ApiResult<Void>> restore(
            @Parameter(description = "Product ID") @PathVariable Long id
    ) {
        restoreProductUseCase.execute(id);
        return ResponseEntity.ok(ApiResult.ok(null, "Product restored successfully"));
    }

    @Operation(summary = "Permanently delete product", description = "Permanently remove a product and all its variants from the database. This action is irreversible.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product permanently deleted"),
            @ApiResponse(responseCode = "404", description = "Product not found", content = @Content(mediaType = "application/json"))
    })
    @DeleteMapping("/{id}/permanent")
    public ResponseEntity<ApiResult<Void>> hardDelete(
            @Parameter(description = "Product ID") @PathVariable Long id
    ) {
        hardDeleteProductUseCase.execute(id);
        return ResponseEntity.ok(ApiResult.ok(null, "Product permanently deleted"));
    }

    // ── Variants ──────────────────────────────────────────────────────────────

    @Operation(summary = "Create variant", description = "Add a new variant to an existing product.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Variant created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input - validation error", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Product not found", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "409", description = "Variant with this SKU already exists", content = @Content(mediaType = "application/json"))
    })
    @PostMapping("/{productId}/variants")
    public ResponseEntity<ApiResult<ProductVariantData>> createVariant(
            @Parameter(description = "Product ID") @PathVariable Long productId,
            @Valid @RequestBody CreateVariantRequest req
    ) {
        ProductVariantData data = createVariantUseCase.execute(productId,
                new CreateVariantCommand(req.sku(), req.name(), req.price(), req.attributes(), req.active()));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.created(data));
    }

    @Operation(summary = "Update variant", description = "Update an existing product variant.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Variant updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input - validation error", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Product or variant not found", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "409", description = "SKU already in use by another variant", content = @Content(mediaType = "application/json"))
    })
    @PutMapping("/{productId}/variants/{variantId}")
    public ResponseEntity<ApiResult<ProductVariantData>> updateVariant(
            @Parameter(description = "Product ID") @PathVariable Long productId,
            @Parameter(description = "Variant ID") @PathVariable Long variantId,
            @Valid @RequestBody UpdateVariantRequest req
    ) {
        ProductVariantData data = updateVariantUseCase.execute(new UpdateVariantCommand(
                variantId, productId, req.sku(), req.name(), req.price(), req.attributes(), req.active()));
        return ResponseEntity.ok(ApiResult.ok(data));
    }

    @Operation(summary = "Delete variant", description = "Remove a variant from a product.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Variant deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Product or variant not found", content = @Content(mediaType = "application/json"))
    })
    @DeleteMapping("/{productId}/variants/{variantId}")
    public ResponseEntity<ApiResult<Void>> deleteVariant(
            @Parameter(description = "Product ID") @PathVariable Long productId,
            @Parameter(description = "Variant ID") @PathVariable Long variantId
    ) {
        deleteVariantUseCase.execute(variantId);
        return ResponseEntity.ok(ApiResult.ok(null, "Variant deleted successfully"));
    }
}
