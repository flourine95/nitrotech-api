package com.nitrotech.api.application.category.controller;

import com.nitrotech.api.application.category.request.CreateCategoryRequest;
import com.nitrotech.api.application.category.request.UpdateCategoryRequest;
import com.nitrotech.api.domain.category.dto.CategoryData;
import com.nitrotech.api.domain.category.dto.CategoryFilter;
import com.nitrotech.api.domain.category.dto.CreateCategoryCommand;
import com.nitrotech.api.domain.category.dto.UpdateCategoryCommand;
import com.nitrotech.api.domain.category.usecase.*;
import com.nitrotech.api.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final GetCategoriesUseCase getCategoriesUseCase;
    private final GetCategoryUseCase getCategoryUseCase;
    private final CreateCategoryUseCase createCategoryUseCase;
    private final UpdateCategoryUseCase updateCategoryUseCase;
    private final DeleteCategoryUseCase deleteCategoryUseCase;

    public CategoryController(GetCategoriesUseCase getCategoriesUseCase,
                               GetCategoryUseCase getCategoryUseCase,
                               CreateCategoryUseCase createCategoryUseCase,
                               UpdateCategoryUseCase updateCategoryUseCase,
                               DeleteCategoryUseCase deleteCategoryUseCase) {
        this.getCategoriesUseCase = getCategoriesUseCase;
        this.getCategoryUseCase = getCategoryUseCase;
        this.createCategoryUseCase = createCategoryUseCase;
        this.updateCategoryUseCase = updateCategoryUseCase;
        this.deleteCategoryUseCase = deleteCategoryUseCase;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Object>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Boolean deleted,
            @RequestParam(required = false) Long parentId,
            @RequestParam(defaultValue = "false") boolean tree,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        if (tree) {
            List<CategoryData> data = getCategoriesUseCase.executeTree(active);
            return ResponseEntity.ok(ApiResponse.ok(data));
        }
        Page<CategoryData> data = getCategoriesUseCase.execute(
                new CategoryFilter(search, active, deleted, parentId), pageable);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryData>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(getCategoryUseCase.execute(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CategoryData>> create(@Valid @RequestBody CreateCategoryRequest request) {
        CategoryData data = createCategoryUseCase.execute(new CreateCategoryCommand(
                request.name(), request.slug(), request.description(),
                request.image(), request.parentId(), request.active()
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(data));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryData>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCategoryRequest request
    ) {
        CategoryData data = updateCategoryUseCase.execute(new UpdateCategoryCommand(
                id, request.name(), request.slug(), request.description(),
                request.image(), request.parentId(), request.active()
        ));
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        deleteCategoryUseCase.execute(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Category deleted successfully"));
    }
}
