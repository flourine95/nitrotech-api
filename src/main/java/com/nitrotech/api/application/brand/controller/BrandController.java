package com.nitrotech.api.application.brand.controller;

import com.nitrotech.api.application.brand.request.BrandListRequest;
import com.nitrotech.api.domain.brand.dto.*;
import com.nitrotech.api.domain.brand.usecase.GetBrandsUseCase;
import com.nitrotech.api.domain.brand.usecase.GetPublicBrandUseCase;
import com.nitrotech.api.shared.response.ApiResult;
import com.nitrotech.api.shared.validation.ValidSortFields;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/brands")
@RequiredArgsConstructor
@Validated
public class BrandController {

    private final GetBrandsUseCase getBrandsUseCase;
    private final GetPublicBrandUseCase getPublicBrandUseCase;

    @GetMapping
    public ResponseEntity<ApiResult<List<BrandData>>> list(
            @Valid @ModelAttribute BrandListRequest filter,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            @ValidSortFields({"id", "name", "slug", "active", "createdAt", "updatedAt"})
            Pageable pageable
    ) {
        var result = getBrandsUseCase.execute(new BrandFilter(filter.getSearch(), true, false), pageable);
        return ResponseEntity.ok(ApiResult.paged(result.page()));
    }

    @GetMapping("/{idOrSlug}")
    public ResponseEntity<ApiResult<BrandData>> get(
            @PathVariable String idOrSlug
    ) {
        return ResponseEntity.ok(ApiResult.ok(getPublicBrandUseCase.execute(idOrSlug)));
    }
}
