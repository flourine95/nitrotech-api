package com.nitrotech.api.application.category.controller;

import com.nitrotech.api.application.category.request.*;
import com.nitrotech.api.domain.category.dto.*;
import com.nitrotech.api.domain.category.usecase.*;
import com.nitrotech.api.shared.response.ApiResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@Tag(name = "Categories", description = "Category management APIs")
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

    @Operation(
            summary = "Get categories",
            description = "Get all categories as tree structure with facets (statistics). Use deleted=true to get soft-deleted categories."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successfully retrieved categories"
            )
    })
    @GetMapping
    public ResponseEntity<ApiResult<List<CategoryData>>> list(
            @Parameter(description = "Set to true to get soft-deleted categories only")
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

    @Operation(
            summary = "Get category by ID",
            description = "Retrieve a single category by its ID, including breadcrumb path and children count."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Category found"),
            @ApiResponse(responseCode = "404", description = "Category not found", content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResult<CategoryData>> get(
            @Parameter(description = "Category ID") @PathVariable Long id
    ) {
        return ResponseEntity.ok(ApiResult.ok(getCategoryUseCase.execute(id)));
    }

    @Operation(
            summary = "Create new category",
            description = "Create a new category. Can be a root category (parentId=null) or a child category."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Category created successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input - validation error",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Category with this slug already exists",
                    content = @Content(mediaType = "application/json")
            )
    })
    @PostMapping
    public ResponseEntity<ApiResult<CategoryData>> create(@Valid @RequestBody CreateCategoryRequest request) {
        CategoryData data = createCategoryUseCase.execute(new CreateCategoryCommand(
                request.name(), request.slug(), request.description(),
                request.image(), request.parentId(), request.active()
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.created(data));
    }

    @Operation(
            summary = "Update category",
            description = "Update an existing category. All fields are optional — only provided fields will be updated."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Category updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input - validation error", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Category not found", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "409", description = "Slug already in use by another category", content = @Content(mediaType = "application/json"))
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResult<CategoryData>> update(
            @Parameter(description = "Category ID") @PathVariable Long id,
            @Valid @RequestBody UpdateCategoryRequest request
    ) {
        CategoryData data = updateCategoryUseCase.execute(new UpdateCategoryCommand(
                id, request.name(), request.slug(), request.description(),
                request.image(), request.parentId(), request.active()
        ));
        return ResponseEntity.ok(ApiResult.ok(data));
    }

    @Operation(
            summary = "Soft delete category",
            description = "Soft delete a category by ID. The category is marked as deleted but remains in the database and can be restored."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Category deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Category not found", content = @Content(mediaType = "application/json"))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResult<Void>> delete(
            @Parameter(description = "Category ID") @PathVariable Long id
    ) {
        deleteCategoryUseCase.execute(id);
        return ResponseEntity.ok(ApiResult.ok(null, "Category deleted successfully"));
    }

    @Operation(
            summary = "Restore soft-deleted category",
            description = "Restore a previously soft-deleted category, making it active again."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Category restored successfully"),
            @ApiResponse(responseCode = "404", description = "Category not found", content = @Content(mediaType = "application/json"))
    })
    @PatchMapping("/{id}/restore")
    public ResponseEntity<ApiResult<Void>> restore(
            @Parameter(description = "Category ID") @PathVariable Long id
    ) {
        restoreCategoryUseCase.execute(id);
        return ResponseEntity.ok(ApiResult.ok(null, "Category restored successfully"));
    }

    @Operation(
            summary = "Permanently delete category",
            description = "Permanently remove a category from the database. This action is irreversible. The category must be soft-deleted first."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Category permanently deleted"),
            @ApiResponse(responseCode = "404", description = "Category not found", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "409", description = "Category still has children or products and cannot be permanently deleted", content = @Content(mediaType = "application/json"))
    })
    @DeleteMapping("/{id}/permanent")
    public ResponseEntity<ApiResult<Void>> hardDelete(
            @Parameter(description = "Category ID") @PathVariable Long id
    ) {
        hardDeleteCategoryUseCase.execute(id);
        return ResponseEntity.ok(ApiResult.ok(null, "Category permanently deleted"));
    }

    @Operation(
            summary = "Move category between parents",
            description = "Move a category from one parent to another, reordering siblings in both source and target. " +
                    "Pass sourceOrderedIds if the source parent changes, targetOrderedIds for the new sibling order."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Category moved successfully, returns all affected categories"),
            @ApiResponse(responseCode = "400", description = "Invalid request - missing required fields", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "404", description = "Category not found", content = @Content(mediaType = "application/json"))
    })
    @PatchMapping("/move")
    public ResponseEntity<ApiResult<MoveCategoryResult>> move(
            @Valid @RequestBody MoveCategoryRequest request) {
        return ResponseEntity.ok(ApiResult.ok(moveCategoryUseCase.execute(
                new MoveCategoryCommand(request.movedId(), request.fromParentId(),
                        request.toParentId(), request.sourceOrderedIds(), request.targetOrderedIds()))));
    }

    @Operation(
            summary = "Bulk soft delete categories",
            description = "Soft delete multiple categories at once (max 100). Returns a summary of succeeded and failed operations."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Bulk delete completed, check succeeded/failed counts in response"),
            @ApiResponse(responseCode = "400", description = "Invalid request - ids must not be empty or exceed 100", content = @Content(mediaType = "application/json"))
    })
    @DeleteMapping("/bulk")
    public ResponseEntity<ApiResult<BulkResult>> bulkDelete(
            @Valid @RequestBody BulkDeleteCategoryRequest request) {
        return ResponseEntity.ok(ApiResult.ok(bulkDeleteCategoryUseCase.execute(request.ids())));
    }

    @Operation(
            summary = "Bulk permanently delete categories",
            description = "Permanently remove multiple categories from the database (max 100). This action is irreversible."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Bulk hard delete completed, check succeeded/failed counts in response"),
            @ApiResponse(responseCode = "400", description = "Invalid request - ids must not be empty or exceed 100", content = @Content(mediaType = "application/json"))
    })
    @DeleteMapping("/bulk/permanent")
    public ResponseEntity<ApiResult<BulkResult>> bulkHardDelete(
            @Valid @RequestBody BulkHardDeleteCategoryRequest request) {
        return ResponseEntity.ok(ApiResult.ok(bulkHardDeleteCategoryUseCase.execute(request.ids())));
    }

    @Operation(
            summary = "Bulk restore categories",
            description = "Restore multiple soft-deleted categories at once (max 100)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Bulk restore completed, check succeeded/failed counts in response"),
            @ApiResponse(responseCode = "400", description = "Invalid request - ids must not be empty or exceed 100", content = @Content(mediaType = "application/json"))
    })
    @PatchMapping("/bulk/restore")
    public ResponseEntity<ApiResult<BulkResult>> bulkRestore(
            @Valid @RequestBody BulkRestoreCategoryRequest request) {
        return ResponseEntity.ok(ApiResult.ok(bulkRestoreCategoryUseCase.execute(request.ids())));
    }

    @Operation(
            summary = "Bulk activate categories",
            description = "Set multiple categories to active status at once (max 100)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Bulk activate completed, check succeeded/failed counts in response"),
            @ApiResponse(responseCode = "400", description = "Invalid request - ids must not be empty or exceed 100", content = @Content(mediaType = "application/json"))
    })
    @PatchMapping("/bulk/activate")
    public ResponseEntity<ApiResult<BulkResult>> bulkActivate(
            @Valid @RequestBody BulkActivateCategoryRequest request) {
        return ResponseEntity.ok(ApiResult.ok(bulkActivateCategoryUseCase.execute(request.ids())));
    }

    @Operation(
            summary = "Bulk deactivate categories",
            description = "Set multiple categories to inactive status at once (max 100)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Bulk deactivate completed, check succeeded/failed counts in response"),
            @ApiResponse(responseCode = "400", description = "Invalid request - ids must not be empty or exceed 100", content = @Content(mediaType = "application/json"))
    })
    @PatchMapping("/bulk/deactivate")
    public ResponseEntity<ApiResult<BulkResult>> bulkDeactivate(
            @Valid @RequestBody BulkDeactivateCategoryRequest request) {
        return ResponseEntity.ok(ApiResult.ok(bulkDeactivateCategoryUseCase.execute(request.ids())));
    }

    @Operation(
            summary = "Validate bulk delete",
            description = "Check which categories can be safely deleted before performing the actual bulk delete. " +
                    "Returns two lists: canDelete (safe to delete) and cannotDelete (blocked, with reasons)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Validation result returned"),
            @ApiResponse(responseCode = "400", description = "Invalid request - ids must not be empty or exceed 100", content = @Content(mediaType = "application/json"))
    })
    @PostMapping("/bulk/validate-delete")
    public ResponseEntity<ApiResult<ValidateDeleteResult>> validateBulkDelete(
            @Valid @RequestBody ValidateDeleteCategoryRequest request) {
        return ResponseEntity.ok(ApiResult.ok(validateBulkDeleteCategoryUseCase.execute(request.ids())));
    }

    @Operation(
            summary = "Move category up",
            description = "Decrease the sort order of a category within its parent, swapping it with the sibling above."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Category moved up successfully"),
            @ApiResponse(responseCode = "404", description = "Category not found", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "409", description = "Category is already at the top position", content = @Content(mediaType = "application/json"))
    })
    @PatchMapping("/{id}/move-up")
    public ResponseEntity<ApiResult<CategoryData>> moveUp(
            @Parameter(description = "Category ID") @PathVariable Long id
    ) {
        return ResponseEntity.ok(ApiResult.ok(moveUpCategoryUseCase.execute(id)));
    }

    @Operation(
            summary = "Move category down",
            description = "Increase the sort order of a category within its parent, swapping it with the sibling below."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Category moved down successfully"),
            @ApiResponse(responseCode = "404", description = "Category not found", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "409", description = "Category is already at the bottom position", content = @Content(mediaType = "application/json"))
    })
    @PatchMapping("/{id}/move-down")
    public ResponseEntity<ApiResult<CategoryData>> moveDown(
            @Parameter(description = "Category ID") @PathVariable Long id
    ) {
        return ResponseEntity.ok(ApiResult.ok(moveDownCategoryUseCase.execute(id)));
    }

    @Operation(
            summary = "Simple move category",
            description = "Move a category to a new parent and/or reposition it relative to a sibling. " +
                    "newParentId=null moves to root. afterId=null places it first; omitting afterId places it last."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Category moved successfully"),
            @ApiResponse(responseCode = "404", description = "Category or target parent not found", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "409", description = "Cannot move category into its own descendant", content = @Content(mediaType = "application/json"))
    })
    @PatchMapping("/{id}/move")
    public ResponseEntity<ApiResult<CategoryData>> simpleMove(
            @Parameter(description = "Category ID") @PathVariable Long id,
            @RequestBody SimpleMoveRequest request) {
        return ResponseEntity.ok(ApiResult.ok(
                simpleMoveCategoryUseCase.execute(id, request.newParentId(), request.afterId())));
    }

    @Operation(
            summary = "Toggle category active status",
            description = "Switch a category between active and inactive. If currently active, it becomes inactive and vice versa."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Category status toggled successfully"),
            @ApiResponse(responseCode = "404", description = "Category not found", content = @Content(mediaType = "application/json"))
    })
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<ApiResult<CategoryData>> toggle(
            @Parameter(description = "Category ID") @PathVariable Long id
    ) {
        return ResponseEntity.ok(ApiResult.ok(toggleCategoryUseCase.execute(id)));
    }
}
