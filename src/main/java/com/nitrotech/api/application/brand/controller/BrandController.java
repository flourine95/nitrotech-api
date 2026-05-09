package com.nitrotech.api.application.brand.controller;

import com.nitrotech.api.application.brand.request.BulkDeleteBrandRequest;
import com.nitrotech.api.application.brand.request.BulkRestoreBrandRequest;
import com.nitrotech.api.application.brand.request.CreateBrandRequest;
import com.nitrotech.api.application.brand.request.UpdateBrandRequest;
import com.nitrotech.api.domain.brand.dto.BrandData;
import com.nitrotech.api.domain.brand.dto.BrandFilter;
import com.nitrotech.api.domain.brand.dto.BulkResult;
import com.nitrotech.api.domain.brand.dto.CreateBrandCommand;
import com.nitrotech.api.domain.brand.dto.UpdateBrandCommand;
import com.nitrotech.api.domain.brand.usecase.*;
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
@RequestMapping("/api/brands")
@Tag(name = "Brands", description = "Brand management APIs")
@RequiredArgsConstructor
public class BrandController {

    private static final Set<String> SORTABLE_FIELDS =
            Set.of("id", "name", "slug", "active", "createdAt", "updatedAt");

    private final GetBrandsUseCase getBrandsUseCase;
    private final GetBrandUseCase getBrandUseCase;
    private final CreateBrandUseCase createBrandUseCase;
    private final UpdateBrandUseCase updateBrandUseCase;
    private final DeleteBrandUseCase deleteBrandUseCase;
    private final RestoreBrandUseCase restoreBrandUseCase;
    private final HardDeleteBrandUseCase hardDeleteBrandUseCase;
    private final BulkDeleteBrandUseCase bulkDeleteBrandUseCase;
    private final BulkRestoreBrandUseCase bulkRestoreBrandUseCase;
    private final BulkHardDeleteBrandUseCase bulkHardDeleteBrandUseCase;

    @Operation(summary = "List brands", description = "Get paginated list of brands with optional filters.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Brands retrieved successfully")
    })
    @GetMapping
    public ResponseEntity<ApiResult<List<BrandData>>> list(
            @Parameter(description = "Search by name or slug") @RequestParam(required = false) String search,
            @Parameter(description = "Filter by active status") @RequestParam(required = false) Boolean active,
            @Parameter(description = "Set to true to include soft-deleted brands") @RequestParam(required = false) Boolean deleted,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort fields, e.g. name,asc") @RequestParam(required = false) List<String> sort
    ) {
        Pageable pageable = SortUtils.toPageable(page, size, sort, SORTABLE_FIELDS, "createdAt");
        var result = getBrandsUseCase.execute(new BrandFilter(search, active, deleted), pageable);
        return ResponseEntity.ok(ApiResult.paged(result.page(), result.facets()));
    }

    @Operation(summary = "Get brand by ID", description = "Retrieve a single brand by its ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Brand found"),
            @ApiResponse(responseCode = "404", description = "Brand not found", content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResult<BrandData>> get(
            @Parameter(description = "Brand ID") @PathVariable Long id
    ) {
        return ResponseEntity.ok(ApiResult.ok(getBrandUseCase.execute(id)));
    }

    @Operation(summary = "Create brand", description = "Create a new brand.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Brand created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input - validation error", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "409", description = "Brand with this slug already exists", content = @Content(mediaType = "application/json"))
    })
    @PostMapping
    public ResponseEntity<ApiResult<BrandData>> create(@Valid @RequestBody CreateBrandRequest request) {
        BrandData data = createBrandUseCase.execute(new CreateBrandCommand(
                request.name(), request.slug(), request.logo(), request.description(), request.active()
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.created(data));
    }

    @Operation(summary = "Update brand", description = "Update an existing brand.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Brand updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input - validation error", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Brand not found", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "409", description = "Slug already in use by another brand", content = @Content(mediaType = "application/json"))
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResult<BrandData>> update(
            @Parameter(description = "Brand ID") @PathVariable Long id,
            @Valid @RequestBody UpdateBrandRequest request
    ) {
        BrandData data = updateBrandUseCase.execute(new UpdateBrandCommand(
                id, request.name(), request.slug(), request.logo(), request.description(), request.active()
        ));
        return ResponseEntity.ok(ApiResult.ok(data));
    }

    @Operation(summary = "Soft delete brand", description = "Soft delete a brand. Can be restored later.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Brand deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Brand not found", content = @Content(mediaType = "application/json"))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResult<Void>> delete(
            @Parameter(description = "Brand ID") @PathVariable Long id
    ) {
        deleteBrandUseCase.execute(id);
        return ResponseEntity.ok(ApiResult.ok(null, "Brand deleted successfully"));
    }

    @Operation(summary = "Restore brand", description = "Restore a previously soft-deleted brand.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Brand restored successfully"),
            @ApiResponse(responseCode = "404", description = "Brand not found", content = @Content(mediaType = "application/json"))
    })
    @PatchMapping("/{id}/restore")
    public ResponseEntity<ApiResult<Void>> restore(
            @Parameter(description = "Brand ID") @PathVariable Long id
    ) {
        restoreBrandUseCase.execute(id);
        return ResponseEntity.ok(ApiResult.ok(null, "Brand restored successfully"));
    }

    @Operation(summary = "Permanently delete brand", description = "Permanently remove a brand from the database. This action is irreversible.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Brand permanently deleted"),
            @ApiResponse(responseCode = "404", description = "Brand not found", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "409", description = "Brand still has products and cannot be permanently deleted", content = @Content(mediaType = "application/json"))
    })
    @DeleteMapping("/{id}/permanent")
    public ResponseEntity<ApiResult<Void>> hardDelete(
            @Parameter(description = "Brand ID") @PathVariable Long id
    ) {
        hardDeleteBrandUseCase.execute(id);
        return ResponseEntity.ok(ApiResult.ok(null, "Brand permanently deleted"));
    }

    @Operation(summary = "Bulk soft delete brands", description = "Soft delete multiple brands at once (max 100).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Bulk delete completed, check succeeded/failed counts in response"),
            @ApiResponse(responseCode = "400", description = "Invalid request - ids must not be empty or exceed 100", content = @Content(mediaType = "application/json"))
    })
    @DeleteMapping("/bulk")
    public ResponseEntity<ApiResult<BulkResult>> bulkDelete(
            @Valid @RequestBody BulkDeleteBrandRequest request) {
        return ResponseEntity.ok(ApiResult.ok(bulkDeleteBrandUseCase.execute(request.ids())));
    }

    @Operation(summary = "Bulk restore brands", description = "Restore multiple soft-deleted brands at once (max 100).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Bulk restore completed, check succeeded/failed counts in response"),
            @ApiResponse(responseCode = "400", description = "Invalid request - ids must not be empty or exceed 100", content = @Content(mediaType = "application/json"))
    })
    @PatchMapping("/bulk/restore")
    public ResponseEntity<ApiResult<BulkResult>> bulkRestore(
            @Valid @RequestBody BulkRestoreBrandRequest request) {
        return ResponseEntity.ok(ApiResult.ok(bulkRestoreBrandUseCase.execute(request.ids())));
    }

    @Operation(summary = "Bulk permanently delete brands", description = "Permanently remove multiple brands from the database (max 100). This action is irreversible.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Bulk hard delete completed, check succeeded/failed counts in response"),
            @ApiResponse(responseCode = "400", description = "Invalid request - ids must not be empty or exceed 100", content = @Content(mediaType = "application/json"))
    })
    @DeleteMapping("/bulk/permanent")
    public ResponseEntity<ApiResult<BulkResult>> bulkHardDelete(
            @Valid @RequestBody BulkDeleteBrandRequest request) {
        return ResponseEntity.ok(ApiResult.ok(bulkHardDeleteBrandUseCase.execute(request.ids())));
    }
}
