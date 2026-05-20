package com.nitrotech.api.application.category.controller;

import com.nitrotech.api.application.category.request.*;
import com.nitrotech.api.domain.category.dto.*;
import com.nitrotech.api.domain.category.usecase.*;
import com.nitrotech.api.shared.response.ApiResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final GetCategoriesUseCase getCategoriesUseCase;
    private final GetCategoryUseCase getCategoryUseCase;
    private final CreateCategoryUseCase createCategoryUseCase;
    private final UpdateCategoryUseCase updateCategoryUseCase;
    private final DeleteCategoryUseCase deleteCategoryUseCase;
    private final RestoreCategoryUseCase restoreCategoryUseCase;
    private final HardDeleteCategoryUseCase hardDeleteCategoryUseCase;
    private final MoveCategoryUseCase moveCategoryUseCase;
    private final BulkDeleteCategoryUseCase bulkDeleteCategoryUseCase;
    private final BulkRestoreCategoryUseCase bulkRestoreCategoryUseCase;
    private final BulkHardDeleteCategoryUseCase bulkHardDeleteCategoryUseCase;
    private final BulkActivateCategoryUseCase bulkActivateCategoryUseCase;
    private final BulkDeactivateCategoryUseCase bulkDeactivateCategoryUseCase;
    private final ValidateBulkDeleteCategoryUseCase validateBulkDeleteCategoryUseCase;
    private final MoveUpCategoryUseCase moveUpCategoryUseCase;
    private final MoveDownCategoryUseCase moveDownCategoryUseCase;
    private final SimpleMoveCategoryUseCase simpleMoveCategoryUseCase;
    private final ToggleCategoryUseCase toggleCategoryUseCase;

    @GetMapping
    public ResponseEntity<ApiResult<List<CategoryData>>> list(
            @RequestParam(required = false) Boolean deleted
    ) {
        if (Boolean.TRUE.equals(deleted)) {
            List<CategoryData> deletedList = getCategoriesUseCase.executeDeleted();
            return ResponseEntity.ok(ApiResult.ok(deletedList));
        } else {
            CategoryTreeResult result = getCategoriesUseCase.executeTreeWithFacets(null);
            return ResponseEntity.ok(new ApiResult<>(result.tree(), null, null, result.facets()));
        }
    }

    @GetMapping("/{idOrSlug}")
    public ResponseEntity<ApiResult<CategoryData>> get(
            @PathVariable String idOrSlug
    ) {
        return ResponseEntity.ok(ApiResult.ok(getCategoryUseCase.execute(idOrSlug)));
    }

    @PostMapping
    public ResponseEntity<ApiResult<CategoryData>> create(@Valid @RequestBody CreateCategoryRequest request) {
        CategoryData data = createCategoryUseCase.execute(new CreateCategoryCommand(
                request.name(), request.slug(), request.description(),
                request.image(), request.parentId(), request.active()
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.created(data));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResult<CategoryData>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCategoryRequest request
    ) {
        CategoryData data = updateCategoryUseCase.execute(new UpdateCategoryCommand(
                id, request.name(), request.slug(), request.description(),
                request.image(), request.parentId(), request.active()
        ));
        return ResponseEntity.ok(ApiResult.ok(data));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResult<Void>> delete(
            @PathVariable Long id
    ) {
        deleteCategoryUseCase.execute(id);
        return ResponseEntity.ok(ApiResult.ok(null, "Category deleted successfully"));
    }

    @PatchMapping("/{id}/restore")
    public ResponseEntity<ApiResult<Void>> restore(
            @PathVariable Long id
    ) {
        restoreCategoryUseCase.execute(id);
        return ResponseEntity.ok(ApiResult.ok(null, "Category restored successfully"));
    }

    @DeleteMapping("/{id}/permanent")
    public ResponseEntity<ApiResult<Void>> hardDelete(
            @PathVariable Long id
    ) {
        hardDeleteCategoryUseCase.execute(id);
        return ResponseEntity.ok(ApiResult.ok(null, "Category permanently deleted"));
    }

    @PatchMapping("/move")
    public ResponseEntity<ApiResult<MoveCategoryResult>> move(
            @Valid @RequestBody MoveCategoryRequest request) {
        return ResponseEntity.ok(ApiResult.ok(moveCategoryUseCase.execute(
                new MoveCategoryCommand(request.movedId(), request.fromParentId(),
                        request.toParentId(), request.sourceOrderedIds(), request.targetOrderedIds()))));
    }

    @DeleteMapping("/bulk")
    public ResponseEntity<ApiResult<BulkResult>> bulkDelete(
            @Valid @RequestBody BulkDeleteCategoryRequest request) {
        return ResponseEntity.ok(ApiResult.ok(bulkDeleteCategoryUseCase.execute(request.ids())));
    }

    @DeleteMapping("/bulk/permanent")
    public ResponseEntity<ApiResult<BulkResult>> bulkHardDelete(
            @Valid @RequestBody BulkHardDeleteCategoryRequest request) {
        return ResponseEntity.ok(ApiResult.ok(bulkHardDeleteCategoryUseCase.execute(request.ids())));
    }

    @PatchMapping("/bulk/restore")
    public ResponseEntity<ApiResult<BulkResult>> bulkRestore(
            @Valid @RequestBody BulkRestoreCategoryRequest request) {
        return ResponseEntity.ok(ApiResult.ok(bulkRestoreCategoryUseCase.execute(request.ids())));
    }

    @PatchMapping("/bulk/activate")
    public ResponseEntity<ApiResult<BulkResult>> bulkActivate(
            @Valid @RequestBody BulkActivateCategoryRequest request) {
        return ResponseEntity.ok(ApiResult.ok(bulkActivateCategoryUseCase.execute(request.ids())));
    }

    @PatchMapping("/bulk/deactivate")
    public ResponseEntity<ApiResult<BulkResult>> bulkDeactivate(
            @Valid @RequestBody BulkDeactivateCategoryRequest request) {
        return ResponseEntity.ok(ApiResult.ok(bulkDeactivateCategoryUseCase.execute(request.ids())));
    }

    @PostMapping("/bulk/validate-delete")
    public ResponseEntity<ApiResult<ValidateDeleteResult>> validateBulkDelete(
            @Valid @RequestBody ValidateDeleteCategoryRequest request) {
        return ResponseEntity.ok(ApiResult.ok(validateBulkDeleteCategoryUseCase.execute(request.ids())));
    }

    @PatchMapping("/{id}/move-up")
    public ResponseEntity<ApiResult<CategoryData>> moveUp(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(ApiResult.ok(moveUpCategoryUseCase.execute(id)));
    }

    @PatchMapping("/{id}/move-down")
    public ResponseEntity<ApiResult<CategoryData>> moveDown(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(ApiResult.ok(moveDownCategoryUseCase.execute(id)));
    }

    @PatchMapping("/{id}/move")
    public ResponseEntity<ApiResult<CategoryData>> simpleMove(
            @PathVariable Long id,
            @RequestBody SimpleMoveRequest request) {
        return ResponseEntity.ok(ApiResult.ok(
                simpleMoveCategoryUseCase.execute(id, request.newParentId(), request.afterId())));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<ApiResult<CategoryData>> toggle(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(ApiResult.ok(toggleCategoryUseCase.execute(id)));
    }
}