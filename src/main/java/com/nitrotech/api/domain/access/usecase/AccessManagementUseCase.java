package com.nitrotech.api.domain.access.usecase;

import com.nitrotech.api.domain.audit.dto.AuditLogCommand;
import com.nitrotech.api.domain.audit.dto.AuditAction;
import com.nitrotech.api.domain.audit.dto.AuditResourceType;
import com.nitrotech.api.domain.audit.service.AuditLogService;
import com.nitrotech.api.domain.access.dto.PermissionData;
import com.nitrotech.api.domain.access.dto.RoleData;
import com.nitrotech.api.domain.access.dto.UserAccessData;
import com.nitrotech.api.domain.access.repository.AccessManagementRepository;
import com.nitrotech.api.shared.exception.BadRequestException;
import com.nitrotech.api.shared.exception.ForbiddenException;
import com.nitrotech.api.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AccessManagementUseCase {

    private static final Set<String> CRITICAL_PERMISSIONS = Set.of("ROLE_MANAGE", "USER_MANAGE_ROLE");

    private final AccessManagementRepository accessRepository;
    private final AuditLogService auditLogService;

    public List<PermissionData> listPermissions() {
        return accessRepository.findPermissions();
    }

    public List<RoleData> listRoles() {
        return accessRepository.findRoles();
    }

    public List<UserAccessData> listUsers() {
        return accessRepository.findUsers();
    }

    @Transactional
    public RoleData createRole(String name, String slug, String description) {
        Long id = accessRepository.createRole(name, slug, description);
        return getRole(id);
    }

    @Transactional
    public RoleData updateRole(Long id, String name, String description, Boolean active) {
        RoleData current = getRole(id);
        if (current.systemRole()) {
            throw new ForbiddenException("SYSTEM_ROLE_PROTECTED", "System roles cannot be edited");
        }
        accessRepository.updateRole(id, name, description, active);
        return getRole(id);
    }

    @Transactional
    public RoleData updateRolePermissions(Long actorId, Long roleId, Set<String> permissionSlugs) {
        RoleData role = getRole(roleId);
        if (role.systemRole()) {
            throw new ForbiddenException("SYSTEM_ROLE_PROTECTED", "System role permissions cannot be edited");
        }

        Set<Long> permissionIds = permissionIds(permissionSlugs);
        guardRolePermissionChange(actorId, roleId, permissionSlugs);

        accessRepository.replaceRolePermissions(roleId, permissionIds);
        RoleData updated = getRole(roleId);
        auditLogService.record(AuditLogCommand.success(
                AuditAction.ROLE_PERMISSION_UPDATED,
                AuditResourceType.ROLE,
                roleId,
                Map.of("permissionSlugs", role.permissionSlugs()),
                Map.of("permissionSlugs", updated.permissionSlugs()),
                Map.of("roleSlug", role.slug())
        ));
        return updated;
    }

    @Transactional
    public UserAccessData updateUserRoles(Long actorId, Long userId, Set<String> roleSlugs) {
        UserAccessData user = getUser(userId);
        Set<Long> roleIds = roleIds(roleSlugs);
        guardUserRoleChange(actorId, userId, roleSlugs);

        accessRepository.replaceUserRoles(userId, roleIds);
        UserAccessData updated = getUser(userId);
        auditLogService.record(AuditLogCommand.success(
                AuditAction.USER_ROLE_UPDATED,
                AuditResourceType.USER,
                userId,
                Map.of("roleSlugs", user.roleSlugs()),
                Map.of("roleSlugs", updated.roleSlugs()),
                Map.of("targetEmail", user.email())
        ));
        return updated;
    }

    private RoleData getRole(Long id) {
        return accessRepository.findRoleById(id)
                .orElseThrow(() -> new NotFoundException("ROLE_NOT_FOUND", "Role not found"));
    }

    private UserAccessData getUser(Long id) {
        return accessRepository.findUserById(id)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "User not found"));
    }

    private Set<Long> permissionIds(Set<String> permissionSlugs) {
        Set<Long> ids = accessRepository.findPermissionIdsBySlugs(permissionSlugs);
        if (ids.size() != permissionSlugs.size()) {
            throw new BadRequestException("PERMISSION_NOT_FOUND", "One or more permissions were not found");
        }
        return ids;
    }

    private Set<Long> roleIds(Set<String> roleSlugs) {
        Set<Long> ids = accessRepository.findRoleIdsBySlugs(roleSlugs);
        if (ids.size() != roleSlugs.size()) {
            throw new BadRequestException("ROLE_NOT_FOUND", "One or more roles were not found");
        }
        return ids;
    }

    private void guardRolePermissionChange(Long actorId, Long roleId, Set<String> newPermissionSlugs) {
        Set<Long> affectedUsers = accessRepository.findUserIdsByRoleId(roleId);

        for (String critical : CRITICAL_PERMISSIONS) {
            if (!newPermissionSlugs.contains(critical)
                    && accessRepository.countUsersWithPermissionExceptRole(roleId, critical) == 0) {
                throw new ForbiddenException("SELF_LOCKOUT_GUARD",
                        "At least one user must keep " + critical);
            }
            if (affectedUsers.contains(actorId)
                    && userWouldLosePermissionAfterRoleChange(actorId, roleId, critical, newPermissionSlugs)) {
                throw new ForbiddenException("SELF_LOCKOUT_GUARD",
                        "You cannot remove your own " + critical + " permission");
            }
        }
    }

    private void guardUserRoleChange(Long actorId, Long userId, Set<String> newRoleSlugs) {
        Set<String> newPermissions = accessRepository.findPermissionSlugsByRoleSlugs(newRoleSlugs);
        for (String critical : CRITICAL_PERMISSIONS) {
            if (!newPermissions.contains(critical)
                    && accessRepository.countUsersWithPermissionExceptUser(userId, critical) == 0) {
                throw new ForbiddenException("SELF_LOCKOUT_GUARD",
                        "At least one user must keep " + critical);
            }
            if (actorId.equals(userId) && !newPermissions.contains(critical)) {
                throw new ForbiddenException("SELF_LOCKOUT_GUARD",
                        "You cannot remove your own " + critical + " permission");
            }
        }
    }

    private boolean userWouldLosePermissionAfterRoleChange(
            Long userId,
            Long changedRoleId,
            String permissionSlug,
            Set<String> newPermissionSlugs
    ) {
        return !newPermissionSlugs.contains(permissionSlug)
                && !accessRepository.userHasPermissionOutsideRole(userId, changedRoleId, permissionSlug);
    }
}
