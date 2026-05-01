package com.nitrotech.api.application.category.controller;

import com.nitrotech.api.application.category.request.CreateCategoryRequest;
import com.nitrotech.api.application.category.request.MoveCategoryRequest;
import com.nitrotech.api.application.category.request.UpdateCategoryRequest;
import com.nitrotech.api.application.category.request.BulkDeleteCategoryRequest;
import com.nitrotech.api.application.category.request.BulkRestoreCategoryRequest;
import com.nitrotech.api.application.category.request.BulkHardDeleteCategoryRequest;
import com.nitrotech.api.application.category.request.BulkActivateCategoryRequest;
import com.nitrotech.api.application.category.request.BulkDeactivateCategoryRequest;
import com.nitrotech.api.application.category.request.ValidateDeleteCategoryRequest;
import com.nitrotech.api.application.category.request.SimpleMoveRequest;
import com.nitrotech.api.domain.category.dto.CategoryData;
import com.nitrotech.api.domain.category.dto.CategoryFilter;
import com.nitrotech.api.domain.category.dto.CreateCategoryCommand;
import com.nitrotech.api.domain.category.dto.MoveCategoryCommand;
import com.nitrotech.api.domain.category.dto.MoveCategoryResult;
import com.nitrotech.api.domain.category.dto.UpdateCategoryCommand;
import com.nitrotech.api.domain.category.dto.BulkResult;
import com.nitrotech.api.domain.category.dto.ValidateDeleteResult;
import com.nitrotech.api.domain.category.usecase.*;
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
@RequestMapping("/api/categories")
public class CategoryController {

    private static final Set<String> SORTABLE_FIELDS =
            Set.of("id", "name", "slug", "active", "createdAt", "updatedAt");

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

    public CategoryController(GetCategoriesUseCase getCategoriesUseCase,
                               GetCategoryUseCase getCategoryUseCase,
                               CreateCategoryUseCase createCategoryUseCase,
                               UpdateCategoryUseCase updateCategoryUseCase,
                               DeleteCategoryUseCase deleteCategoryUseCase,
                               RestoreCategoryUseCase restoreCategoryUseCase,
                               HardDeleteCategoryUseCase hardDeleteCategoryUseCase,
                               MoveCategoryUseCase moveCategoryUseCase,
                               BulkDeleteCategoryUseCase bulkDeleteCategoryUseCase,
                               BulkRestoreCategoryUseCase bulkRestoreCategoryUseCase,
                               BulkHardDeleteCategoryUseCase bulkHardDeleteCategoryUseCase,
                               BulkActivateCategoryUseCase bulkActivateCategoryUseCase,
                               BulkDeactivateCategoryUseCase bulkDeactivateCategoryUseCase,
                               ValidateBulkDeleteCategoryUseCase validateBulkDeleteCategoryUseCase,
                               MoveUpCategoryUseCase moveUpCategoryUseCase,
                               MoveDownCategoryUseCase moveDownCategoryUseCase,
                               SimpleMoveCategoryUseCase simpleMoveCategoryUseCase) {
        this.getCategoriesUseCase = getCategoriesUseCase;
        this.getCategoryUseCase = getCategoryUseCase;
        this.createCategoryUseCase = createCategoryUseCase;
        this.updateCategoryUseCase = updateCategoryUseCase;
        this.deleteCategoryUseCase = deleteCategoryUseCase;
        this.restoreCategoryUseCase = restoreCategoryUseCase;
        this.hardDeleteCategoryUseCase = hardDeleteCategoryUseCase;
        this.moveCategoryUseCase = moveCategoryUseCase;
        this.bulkDeleteCategoryUseCase = bulkDeleteCategoryUseCase;
        this.bulkRestoreCategoryUseCase = bulkRestoreCategoryUseCase;
        this.bulkHardDeleteCategoryUseCase = bulkHardDeleteCategoryUseCase;
        this.bulkActivateCategoryUseCase = bulkActivateCategoryUseCase;
        this.bulkDeactivateCategoryUseCase = bulkDeactivateCategoryUseCase;
        this.validateBulkDeleteCategoryUseCase = validateBulkDeleteCategoryUseCase;
        this.moveUpCategoryUseCase = moveUpCategoryUseCase;
        this.moveDownCategoryUseCase = moveDownCategoryUseCase;
        this.simpleMoveCategoryUseCase = simpleMoveCategoryUseCase;
    }

    @GetMapping
    @SuppressWarnings("unchecked")
    public ResponseEntity<?> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Boolean deleted,
            @RequestParam(required = false) Long parentId,
            @RequestParam(defaultValue = "false") boolean tree,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) List<String> sort
    ) {
        if (tree) {
            return ResponseEntity.ok(ApiResponse.ok(getCategoriesUseCase.executeTree(active)));
        }
        Pageable pageable = SortUtils.toPageable(page, size, sort, SORTABLE_FIELDS, "createdAt");
        return ResponseEntity.ok(ApiResponse.paged(
                getCategoriesUseCase.execute(new CategoryFilter(search, active, deleted, parentId), pageable)));
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

    @PatchMapping("/{id}/restore")
    public ResponseEntity<ApiResponse<Void>> restore(@PathVariable Long id) {
        restoreCategoryUseCase.execute(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Category restored successfully"));
    }

    @DeleteMapping("/{id}/permanent")
    public ResponseEntity<ApiResponse<Void>> hardDelete(@PathVariable Long id) {
        hardDeleteCategoryUseCase.execute(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Category permanently deleted"));
    }

    @PatchMapping("/move")
    public ResponseEntity<ApiResponse<MoveCategoryResult>> move(
            @Valid @RequestBody MoveCategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(moveCategoryUseCase.execute(
                new MoveCategoryCommand(request.movedId(), request.fromParentId(),
                        request.toParentId(), request.sourceOrderedIds(), request.targetOrderedIds()))));
    }

    @DeleteMapping("/bulk")
    public ResponseEntity<ApiResponse<BulkResult>> bulkDelete(
            @Valid @RequestBody BulkDeleteCategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(bulkDeleteCategoryUseCase.execute(request.ids())));
    }

    @DeleteMapping("/bulk/permanent")
    public ResponseEntity<ApiResponse<BulkResult>> bulkHardDelete(
            @Valid @RequestBody BulkHardDeleteCategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(bulkHardDeleteCategoryUseCase.execute(request.ids())));
    }

    @PatchMapping("/bulk/restore")
    public ResponseEntity<ApiResponse<BulkResult>> bulkRestore(
            @Valid @RequestBody BulkRestoreCategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(bulkRestoreCategoryUseCase.execute(request.ids())));
    }

    @PatchMapping("/bulk/activate")
    public ResponseEntity<ApiResponse<BulkResult>> bulkActivate(
            @Valid @RequestBody BulkActivateCategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(bulkActivateCategoryUseCase.execute(request.ids())));
    }

    @PatchMapping("/bulk/deactivate")
    public ResponseEntity<ApiResponse<BulkResult>> bulkDeactivate(
            @Valid @RequestBody BulkDeactivateCategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(bulkDeactivateCategoryUseCase.execute(request.ids())));
    }

    @PostMapping("/bulk/validate-delete")
    public ResponseEntity<ApiResponse<ValidateDeleteResult>> validateBulkDelete(
            @Valid @RequestBody ValidateDeleteCategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(validateBulkDeleteCategoryUseCase.execute(request.ids())));
    }

    @PatchMapping("/{id}/move-up")
    public ResponseEntity<ApiResponse<CategoryData>> moveUp(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(moveUpCategoryUseCase.execute(id)));
    }

    @PatchMapping("/{id}/move-down")
    public ResponseEntity<ApiResponse<CategoryData>> moveDown(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(moveDownCategoryUseCase.execute(id)));
    }

    @PatchMapping("/{id}/move")
    public ResponseEntity<ApiResponse<CategoryData>> simpleMove(
            @PathVariable Long id,
            @RequestBody SimpleMoveRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                simpleMoveCategoryUseCase.execute(id, request.newParentId(), request.afterId())));
    }
}
