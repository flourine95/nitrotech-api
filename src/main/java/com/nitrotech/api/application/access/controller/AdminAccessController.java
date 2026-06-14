package com.nitrotech.api.application.access.controller;

import com.nitrotech.api.application.access.request.CreateRoleRequest;
import com.nitrotech.api.application.access.request.UpdateRolePermissionsRequest;
import com.nitrotech.api.application.access.request.UpdateRoleRequest;
import com.nitrotech.api.application.access.request.UpdateUserRolesRequest;
import com.nitrotech.api.domain.access.dto.PermissionData;
import com.nitrotech.api.domain.access.dto.RoleData;
import com.nitrotech.api.domain.access.dto.UserAccessData;
import com.nitrotech.api.domain.access.usecase.AccessManagementUseCase;
import com.nitrotech.api.shared.response.ApiResult;
import com.nitrotech.api.shared.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/access")
@RequiredArgsConstructor
public class AdminAccessController {

    private final AccessManagementUseCase accessManagementUseCase;

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('ROLE_READ')")
    public ResponseEntity<ApiResult<List<PermissionData>>> listPermissions() {
        return ResponseEntity.ok(ApiResult.ok(accessManagementUseCase.listPermissions()));
    }

    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('ROLE_READ')")
    public ResponseEntity<ApiResult<List<RoleData>>> listRoles() {
        return ResponseEntity.ok(ApiResult.ok(accessManagementUseCase.listRoles()));
    }

    @PostMapping("/roles")
    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    public ResponseEntity<ApiResult<RoleData>> createRole(@Valid @RequestBody CreateRoleRequest request) {
        RoleData role = accessManagementUseCase.createRole(request.name(), request.slug(), request.description());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResult.created(role));
    }

    @PutMapping("/roles/{id}")
    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    public ResponseEntity<ApiResult<RoleData>> updateRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRoleRequest request
    ) {
        return ResponseEntity.ok(ApiResult.ok(accessManagementUseCase.updateRole(
                id, request.name(), request.description(), request.active()
        )));
    }

    @PutMapping("/roles/{id}/permissions")
    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    public ResponseEntity<ApiResult<RoleData>> updateRolePermissions(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateRolePermissionsRequest request
    ) {
        return ResponseEntity.ok(ApiResult.ok(accessManagementUseCase.updateRolePermissions(
                principal.id(), id, request.permissionSlugs()
        )));
    }

    @GetMapping("/users")
    @PreAuthorize("hasAuthority('USER_READ')")
    public ResponseEntity<ApiResult<List<UserAccessData>>> listUsers() {
        return ResponseEntity.ok(ApiResult.ok(accessManagementUseCase.listUsers()));
    }

    @PutMapping("/users/{id}/roles")
    @PreAuthorize("hasAuthority('USER_MANAGE_ROLE')")
    public ResponseEntity<ApiResult<UserAccessData>> updateUserRoles(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRolesRequest request
    ) {
        return ResponseEntity.ok(ApiResult.ok(accessManagementUseCase.updateUserRoles(
                principal.id(), id, request.roleSlugs()
        )));
    }
}
