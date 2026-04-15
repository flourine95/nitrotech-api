package com.nitrotech.api.application.brand.controller;

import com.nitrotech.api.application.brand.request.CreateBrandRequest;
import com.nitrotech.api.application.brand.request.UpdateBrandRequest;
import com.nitrotech.api.domain.brand.dto.BrandData;
import com.nitrotech.api.domain.brand.dto.BrandFilter;
import com.nitrotech.api.domain.brand.dto.CreateBrandCommand;
import com.nitrotech.api.domain.brand.dto.UpdateBrandCommand;
import com.nitrotech.api.domain.brand.usecase.*;
import com.nitrotech.api.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/brands")
public class BrandController {

    private final GetBrandsUseCase getBrandsUseCase;
    private final GetBrandUseCase getBrandUseCase;
    private final CreateBrandUseCase createBrandUseCase;
    private final UpdateBrandUseCase updateBrandUseCase;
    private final DeleteBrandUseCase deleteBrandUseCase;
    private final RestoreBrandUseCase restoreBrandUseCase;
    private final HardDeleteBrandUseCase hardDeleteBrandUseCase;

    public BrandController(GetBrandsUseCase getBrandsUseCase, GetBrandUseCase getBrandUseCase,
                            CreateBrandUseCase createBrandUseCase, UpdateBrandUseCase updateBrandUseCase,
                            DeleteBrandUseCase deleteBrandUseCase,
                            RestoreBrandUseCase restoreBrandUseCase,
                            HardDeleteBrandUseCase hardDeleteBrandUseCase) {
        this.getBrandsUseCase = getBrandsUseCase;
        this.getBrandUseCase = getBrandUseCase;
        this.createBrandUseCase = createBrandUseCase;
        this.updateBrandUseCase = updateBrandUseCase;
        this.deleteBrandUseCase = deleteBrandUseCase;
        this.restoreBrandUseCase = restoreBrandUseCase;
        this.hardDeleteBrandUseCase = hardDeleteBrandUseCase;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<BrandData>>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Boolean deleted,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                getBrandsUseCase.execute(new BrandFilter(search, active, deleted), pageable)));
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
}
