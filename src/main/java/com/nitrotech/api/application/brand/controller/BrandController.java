package com.nitrotech.api.application.brand.controller;

import com.nitrotech.api.application.brand.request.CreateBrandRequest;
import com.nitrotech.api.application.brand.request.UpdateBrandRequest;
import com.nitrotech.api.domain.brand.dto.BrandData;
import com.nitrotech.api.domain.brand.dto.CreateBrandCommand;
import com.nitrotech.api.domain.brand.dto.UpdateBrandCommand;
import com.nitrotech.api.domain.brand.usecase.*;
import com.nitrotech.api.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/brands")
public class BrandController {

    private final GetBrandsUseCase getBrandsUseCase;
    private final GetBrandUseCase getBrandUseCase;
    private final CreateBrandUseCase createBrandUseCase;
    private final UpdateBrandUseCase updateBrandUseCase;
    private final DeleteBrandUseCase deleteBrandUseCase;

    public BrandController(GetBrandsUseCase getBrandsUseCase, GetBrandUseCase getBrandUseCase,
                            CreateBrandUseCase createBrandUseCase, UpdateBrandUseCase updateBrandUseCase,
                            DeleteBrandUseCase deleteBrandUseCase) {
        this.getBrandsUseCase = getBrandsUseCase;
        this.getBrandUseCase = getBrandUseCase;
        this.createBrandUseCase = createBrandUseCase;
        this.updateBrandUseCase = updateBrandUseCase;
        this.deleteBrandUseCase = deleteBrandUseCase;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BrandData>>> list(
            @RequestParam(required = false) Boolean active
    ) {
        return ResponseEntity.ok(ApiResponse.ok(getBrandsUseCase.execute(active)));
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
}
