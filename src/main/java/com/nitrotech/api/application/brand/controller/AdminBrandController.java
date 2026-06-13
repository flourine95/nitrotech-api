package com.nitrotech.api.application.brand.controller;

import com.nitrotech.api.application.brand.request.BrandListRequest;
import com.nitrotech.api.application.brand.request.BulkDeleteBrandRequest;
import com.nitrotech.api.application.brand.request.BulkRestoreBrandRequest;
import com.nitrotech.api.application.brand.request.CreateBrandRequest;
import com.nitrotech.api.application.brand.request.UpdateBrandRequest;
import com.nitrotech.api.domain.brand.dto.*;
import com.nitrotech.api.domain.brand.usecase.*;
import com.nitrotech.api.shared.response.ApiResult;
import com.nitrotech.api.shared.validation.ValidSortFields;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/brands")
@RequiredArgsConstructor
@Validated
public class AdminBrandController {

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

    @GetMapping
    @PreAuthorize("hasAuthority('BRAND_READ')")
    public ResponseEntity<ApiResult<List<BrandData>>> list(
            @Valid @ModelAttribute BrandListRequest filter,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            @ValidSortFields({"id", "name", "slug", "active", "createdAt", "updatedAt"})
            Pageable pageable
    ) {
        var result = getBrandsUseCase.execute(filter.toFilter(), pageable);
        return ResponseEntity.ok(ApiResult.paged(result.page(), result.facets()));
    }

    @GetMapping("/{idOrSlug}")
    @PreAuthorize("hasAuthority('BRAND_READ')")
    public ResponseEntity<ApiResult<BrandData>> get(@PathVariable String idOrSlug) {
        return ResponseEntity.ok(ApiResult.ok(getBrandUseCase.execute(idOrSlug)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('BRAND_CREATE')")
    public ResponseEntity<ApiResult<BrandData>> create(@Valid @RequestBody CreateBrandRequest request) {
        BrandData data = createBrandUseCase.execute(new CreateBrandCommand(
                request.name(), request.slug(), request.logo(), request.description(), request.active()
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.created(data));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('BRAND_UPDATE')")
    public ResponseEntity<ApiResult<BrandData>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBrandRequest request
    ) {
        BrandData data = updateBrandUseCase.execute(new UpdateBrandCommand(
                id, request.name(), request.slug(), request.logo(), request.description(), request.active()
        ));
        return ResponseEntity.ok(ApiResult.ok(data));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('BRAND_DELETE')")
    public ResponseEntity<ApiResult<Void>> delete(@PathVariable Long id) {
        deleteBrandUseCase.execute(id);
        return ResponseEntity.ok(ApiResult.ok("Brand deleted successfully"));
    }

    @PatchMapping("/{id}/restore")
    @PreAuthorize("hasAuthority('BRAND_UPDATE')")
    public ResponseEntity<ApiResult<Void>> restore(@PathVariable Long id) {
        restoreBrandUseCase.execute(id);
        return ResponseEntity.ok(ApiResult.ok("Brand restored successfully"));
    }

    @DeleteMapping("/{id}/permanent")
    @PreAuthorize("hasAuthority('BRAND_DELETE')")
    public ResponseEntity<ApiResult<Void>> hardDelete(@PathVariable Long id) {
        hardDeleteBrandUseCase.execute(id);
        return ResponseEntity.ok(ApiResult.ok("Brand permanently deleted"));
    }

    @DeleteMapping("/bulk")
    @PreAuthorize("hasAuthority('BRAND_DELETE')")
    public ResponseEntity<ApiResult<BulkResult>> bulkDelete(
            @Valid @RequestBody BulkDeleteBrandRequest request) {
        return ResponseEntity.ok(ApiResult.ok(bulkDeleteBrandUseCase.execute(request.ids())));
    }

    @PatchMapping("/bulk/restore")
    @PreAuthorize("hasAuthority('BRAND_UPDATE')")
    public ResponseEntity<ApiResult<BulkResult>> bulkRestore(
            @Valid @RequestBody BulkRestoreBrandRequest request) {
        return ResponseEntity.ok(ApiResult.ok(bulkRestoreBrandUseCase.execute(request.ids())));
    }

    @DeleteMapping("/bulk/permanent")
    @PreAuthorize("hasAuthority('BRAND_DELETE')")
    public ResponseEntity<ApiResult<BulkResult>> bulkHardDelete(
            @Valid @RequestBody BulkDeleteBrandRequest request) {
        return ResponseEntity.ok(ApiResult.ok(bulkHardDeleteBrandUseCase.execute(request.ids())));
    }
}
