package com.nitrotech.api.domain.access.repository;

import com.nitrotech.api.domain.access.dto.PermissionData;
import com.nitrotech.api.domain.access.dto.RoleData;
import com.nitrotech.api.domain.access.dto.UserAccessData;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface AccessManagementRepository {

    List<PermissionData> findPermissions();

    List<RoleData> findRoles();

    List<UserAccessData> findUsers();

    Optional<RoleData> findRoleById(Long id);

    Optional<UserAccessData> findUserById(Long id);

    Long createRole(String name, String slug, String description);

    void updateRole(Long id, String name, String description, Boolean active);

    Set<Long> findPermissionIdsBySlugs(Set<String> permissionSlugs);

    Set<Long> findRoleIdsBySlugs(Set<String> roleSlugs);

    void replaceRolePermissions(Long roleId, Set<Long> permissionIds);

    void replaceUserRoles(Long userId, Set<Long> roleIds);

    Set<Long> findUserIdsByRoleId(Long roleId);

    int countUsersWithPermissionExceptRole(Long roleId, String permissionSlug);

    int countUsersWithPermissionExceptUser(Long userId, String permissionSlug);

    boolean userHasPermissionOutsideRole(Long userId, Long roleId, String permissionSlug);

    Set<String> findPermissionSlugsByRoleSlugs(Set<String> roleSlugs);

    List<String> findEmailsByRoleId(Long roleId);
}
