package com.nitrotech.api.application.category.controller;

import com.nitrotech.api.domain.category.dto.*;
import com.nitrotech.api.domain.category.usecase.GetCategoriesUseCase;
import com.nitrotech.api.domain.category.usecase.GetPublicCategoryUseCase;
import com.nitrotech.api.shared.response.ApiResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final GetCategoriesUseCase getCategoriesUseCase;
    private final GetPublicCategoryUseCase getPublicCategoryUseCase;

    @GetMapping
    public ResponseEntity<ApiResult<List<CategoryData>>> list() {
        return ResponseEntity.ok(ApiResult.ok(getCategoriesUseCase.executeTree(true)));
    }

    @GetMapping("/{idOrSlug}")
    public ResponseEntity<ApiResult<CategoryData>> get(
            @PathVariable String idOrSlug
    ) {
        return ResponseEntity.ok(ApiResult.ok(getPublicCategoryUseCase.execute(idOrSlug)));
    }
}
