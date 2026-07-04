package com.nitrotech.api.application.user.controller;

import com.nitrotech.api.application.user.request.AdminUserListRequest;
import com.nitrotech.api.application.user.request.BulkDeleteUsersRequest;
import com.nitrotech.api.application.user.request.BulkRestoreUsersRequest;
import com.nitrotech.api.application.user.request.BulkUpdateUserStatusRequest;
import com.nitrotech.api.application.user.request.CreateAdminUserRequest;
import com.nitrotech.api.application.user.request.UpdateAdminUserRequest;
import com.nitrotech.api.domain.user.dto.AdminUserData;
import com.nitrotech.api.domain.user.dto.AdminUserFacets;
import com.nitrotech.api.domain.user.dto.BulkResult;
import com.nitrotech.api.domain.user.dto.UserImportResult;
import com.nitrotech.api.domain.user.usecase.BulkDeleteUsersUseCase;
import com.nitrotech.api.domain.user.usecase.BulkRestoreUsersUseCase;
import com.nitrotech.api.domain.user.usecase.BulkUpdateUserStatusUseCase;
import com.nitrotech.api.domain.user.usecase.CreateAdminUserUseCase;
import com.nitrotech.api.domain.user.usecase.GetAdminUserFacetsUseCase;
import com.nitrotech.api.domain.user.usecase.GetAdminUsersUseCase;
import com.nitrotech.api.domain.user.usecase.ImportAdminUsersUseCase;
import com.nitrotech.api.domain.user.usecase.ResetAdminUserPasswordUseCase;
import com.nitrotech.api.domain.user.usecase.UpdateAdminUserUseCase;
import com.nitrotech.api.shared.response.ApiResult;
import com.nitrotech.api.shared.security.UserPrincipal;
import com.nitrotech.api.shared.validation.ValidSortFields;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Validated
public class AdminUserController {

    private final GetAdminUsersUseCase getAdminUsersUseCase;
    private final GetAdminUserFacetsUseCase getAdminUserFacetsUseCase;
    private final BulkDeleteUsersUseCase bulkDeleteUsersUseCase;
    private final BulkUpdateUserStatusUseCase bulkUpdateUserStatusUseCase;
    private final BulkRestoreUsersUseCase bulkRestoreUsersUseCase;
    private final CreateAdminUserUseCase createAdminUserUseCase;
    private final UpdateAdminUserUseCase updateAdminUserUseCase;
    private final ResetAdminUserPasswordUseCase resetAdminUserPasswordUseCase;
    private final ImportAdminUsersUseCase importAdminUsersUseCase;

    @GetMapping
    @PreAuthorize("hasAuthority('USER_READ')")
    public ResponseEntity<ApiResult<List<AdminUserData>>> list(
            @Valid @ModelAttribute AdminUserListRequest request,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            @ValidSortFields({
                    "id", "name", "email", "status", "provider", "orderCount",
                    "totalSpent", "averageOrderValue", "lastOrderAt", "createdAt", "updatedAt"
            })
            Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResult.paged(getAdminUsersUseCase.execute(request.toFilter(), pageable)));
    }

    @GetMapping("/facets")
    @PreAuthorize("hasAuthority('USER_READ')")
    public ResponseEntity<ApiResult<AdminUserFacets>> facets(
            @Valid @ModelAttribute AdminUserListRequest request
    ) {
        return ResponseEntity.ok(ApiResult.ok(getAdminUserFacetsUseCase.execute(request.toFilter())));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('USER_CREATE')")
    public ResponseEntity<ApiResult<AdminUserData>> create(@Valid @RequestBody CreateAdminUserRequest request) {
        return ResponseEntity.ok(ApiResult.created(createAdminUserUseCase.execute(
                request.name(), request.email(), request.phone(), request.status(), request.roleSlugs()
        )));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ResponseEntity<ApiResult<AdminUserData>> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateAdminUserRequest request
    ) {
        return ResponseEntity.ok(ApiResult.ok(updateAdminUserUseCase.execute(
                id, request.name(), request.email(), request.phone(), request.status(), principal.id()
        )));
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ResponseEntity<ApiResult<Void>> resetPassword(@PathVariable Long id) {
        resetAdminUserPasswordUseCase.execute(id);
        return ResponseEntity.ok(ApiResult.ok("Password reset email sent"));
    }

    @PostMapping("/import")
    @PreAuthorize("hasAuthority('USER_CREATE')")
    public ResponseEntity<ApiResult<UserImportResult>> importUsers(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResult.ok(importAdminUsersUseCase.execute(file)));
    }

    @PutMapping("/bulk/status")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ResponseEntity<ApiResult<BulkResult>> updateStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody BulkUpdateUserStatusRequest request
    ) {
        return ResponseEntity.ok(ApiResult.ok(bulkUpdateUserStatusUseCase.execute(request.ids(), request.status(), principal.id())));
    }

    @DeleteMapping("/bulk")
    @PreAuthorize("hasAuthority('USER_DELETE')")
    public ResponseEntity<ApiResult<BulkResult>> delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody BulkDeleteUsersRequest request
    ) {
        return ResponseEntity.ok(ApiResult.ok(bulkDeleteUsersUseCase.execute(request.ids(), principal.id())));
    }

    @PatchMapping("/bulk/restore")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ResponseEntity<ApiResult<BulkResult>> restore(
            @Valid @RequestBody BulkRestoreUsersRequest request
    ) {
        return ResponseEntity.ok(ApiResult.ok(bulkRestoreUsersUseCase.execute(request.ids())));
    }
}
