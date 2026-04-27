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
import com.nitrotech.api.shared.response.ApiResponse;
import com.nitrotech.api.shared.util.SortUtils;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/brands")
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

    public BrandController(GetBrandsUseCase getBrandsUseCase, GetBrandUseCase getBrandUseCase,
                            CreateBrandUseCase createBrandUseCase, UpdateBrandUseCase updateBrandUseCase,
                            DeleteBrandUseCase deleteBrandUseCase,
                            RestoreBrandUseCase restoreBrandUseCase,
                            HardDeleteBrandUseCase hardDeleteBrandUseCase,
                            BulkDeleteBrandUseCase bulkDeleteBrandUseCase,
                            BulkRestoreBrandUseCase bulkRestoreBrandUseCase,
                            BulkHardDeleteBrandUseCase bulkHardDeleteBrandUseCase) {
        this.getBrandsUseCase = getBrandsUseCase;
        this.getBrandUseCase = getBrandUseCase;
        this.createBrandUseCase = createBrandUseCase;
        this.updateBrandUseCase = updateBrandUseCase;
        this.deleteBrandUseCase = deleteBrandUseCase;
        this.restoreBrandUseCase = restoreBrandUseCase;
        this.hardDeleteBrandUseCase = hardDeleteBrandUseCase;
        this.bulkDeleteBrandUseCase = bulkDeleteBrandUseCase;
        this.bulkRestoreBrandUseCase = bulkRestoreBrandUseCase;
        this.bulkHardDeleteBrandUseCase = bulkHardDeleteBrandUseCase;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BrandData>>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Boolean deleted,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) List<String> sort
    ) {
        Pageable pageable = SortUtils.toPageable(page, size, sort, SORTABLE_FIELDS, "createdAt");
        var result = getBrandsUseCase.execute(new BrandFilter(search, active, deleted), pageable);
        return ResponseEntity.ok(ApiResponse.paged(result.page(), result.facets()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BrandData>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(getBrandUseCase.execute(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BrandData>> create(@Valid @RequestBody CreateBrandRequest request) {
        BrandData data = createBrandUseCase.execute(new CreateBrandCommand(
                request.name(), request.slug(), request.logo(), request.description(), request.active()
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(data));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BrandData>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBrandRequest request
    ) {
        BrandData data = updateBrandUseCase.execute(new UpdateBrandCommand(
                id, request.name(), request.slug(), request.logo(), request.description(), request.active()
        ));
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        deleteBrandUseCase.execute(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Brand deleted successfully"));
    }

    @PatchMapping("/{id}/restore")
    public ResponseEntity<ApiResponse<Void>> restore(@PathVariable Long id) {
        restoreBrandUseCase.execute(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Brand restored successfully"));
    }

    @DeleteMapping("/{id}/permanent")
    public ResponseEntity<ApiResponse<Void>> hardDelete(@PathVariable Long id) {
        hardDeleteBrandUseCase.execute(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Brand permanently deleted"));
    }

    @DeleteMapping("/bulk")
    public ResponseEntity<ApiResponse<BulkResult>> bulkDelete(
            @Valid @RequestBody BulkDeleteBrandRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(bulkDeleteBrandUseCase.execute(request.ids())));
    }

    @PatchMapping("/bulk/restore")
    public ResponseEntity<ApiResponse<BulkResult>> bulkRestore(
            @Valid @RequestBody BulkRestoreBrandRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(bulkRestoreBrandUseCase.execute(request.ids())));
    }

    @DeleteMapping("/bulk/permanent")
    public ResponseEntity<ApiResponse<BulkResult>> bulkHardDelete(
            @Valid @RequestBody BulkDeleteBrandRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(bulkHardDeleteBrandUseCase.execute(request.ids())));
    }

}
