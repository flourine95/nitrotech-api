package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.domain.access.dto.PermissionData;
import com.nitrotech.api.domain.access.dto.RoleData;
import com.nitrotech.api.domain.access.dto.UserAccessData;
import com.nitrotech.api.domain.access.repository.AccessManagementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class AccessManagementRepositoryImpl implements AccessManagementRepository {

    private final JdbcTemplate jdbc;

    @Override
    public List<PermissionData> findPermissions() {
        return jdbc.query("""
                SELECT id, name, slug, group_name, description, system_permission
                FROM permissions
                WHERE deleted_at IS NULL
                ORDER BY group_name, slug
                """, (rs, rowNum) -> new PermissionData(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("slug"),
                rs.getString("group_name"),
                rs.getString("description"),
                rs.getBoolean("system_permission")
        ));
    }

    @Override
    public List<RoleData> findRoles() {
        return jdbc.query("""
                SELECT id, name, slug, description, active, system_role
                FROM roles
                WHERE deleted_at IS NULL
                ORDER BY slug
                """, (rs, rowNum) -> toRoleData(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("slug"),
                rs.getString("description"),
                rs.getBoolean("active"),
                rs.getBoolean("system_role")
        ));
    }

    @Override
    public List<UserAccessData> findUsers() {
        return jdbc.query("""
                SELECT id, name, email, status
                FROM users
                WHERE deleted_at IS NULL
                ORDER BY id
                """, (rs, rowNum) -> toUserAccessData(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("email"),
                rs.getString("status")
        ));
    }

    @Override
    public Optional<RoleData> findRoleById(Long id) {
        List<RoleData> roles = jdbc.query("""
                SELECT id, name, slug, description, active, system_role
                FROM roles
                WHERE id = ?
                  AND deleted_at IS NULL
                """, (rs, rowNum) -> toRoleData(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("slug"),
                rs.getString("description"),
                rs.getBoolean("active"),
                rs.getBoolean("system_role")
        ), id);
        return roles.stream().findFirst();
    }

    @Override
    public Optional<UserAccessData> findUserById(Long id) {
        List<UserAccessData> users = jdbc.query("""
                SELECT id, name, email, status
                FROM users
                WHERE id = ?
                  AND deleted_at IS NULL
                """, (rs, rowNum) -> toUserAccessData(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("email"),
                rs.getString("status")
        ), id);
        return users.stream().findFirst();
    }

    @Override
    public Long createRole(String name, String slug, String description) {
        return jdbc.queryForObject("""
                INSERT INTO roles (name, slug, description, system_role)
                VALUES (?, ?, ?, FALSE)
                RETURNING id
                """, Long.class, name, slug, description);
    }

    @Override
    public void updateRole(Long id, String name, String description, Boolean active) {
        jdbc.update("""
                UPDATE roles
                SET name = ?,
                    description = ?,
                    active = COALESCE(?, active),
                    updated_at = NOW()
                WHERE id = ?
                """, name, description, active, id);
    }

    @Override
    public Set<Long> findPermissionIdsBySlugs(Set<String> permissionSlugs) {
        Set<Long> ids = new LinkedHashSet<>();
        for (String slug : permissionSlugs) {
            ids.addAll(jdbc.queryForList("""
                    SELECT id
                    FROM permissions
                    WHERE slug = ?
                      AND deleted_at IS NULL
                    """, Long.class, slug));
        }
        return ids;
    }

    @Override
    public Set<Long> findRoleIdsBySlugs(Set<String> roleSlugs) {
        Set<Long> ids = new LinkedHashSet<>();
        for (String slug : roleSlugs) {
            ids.addAll(jdbc.queryForList("""
                    SELECT id
                    FROM roles
                    WHERE slug = ?
                      AND active = TRUE
                      AND deleted_at IS NULL
                    """, Long.class, slug));
        }
        return ids;
    }

    @Override
    public void replaceRolePermissions(Long roleId, Set<Long> permissionIds) {
        jdbc.update("DELETE FROM role_permissions WHERE role_id = ?", roleId);
        for (Long permissionId : permissionIds) {
            jdbc.update("""
                    INSERT INTO role_permissions (role_id, permission_id)
                    VALUES (?, ?)
                    ON CONFLICT DO NOTHING
                    """, roleId, permissionId);
        }
    }

    @Override
    public void replaceUserRoles(Long userId, Set<Long> roleIds) {
        jdbc.update("DELETE FROM user_roles WHERE user_id = ?", userId);
        for (Long roleId : roleIds) {
            jdbc.update("""
                    INSERT INTO user_roles (user_id, role_id)
                    VALUES (?, ?)
                    ON CONFLICT DO NOTHING
                    """, userId, roleId);
        }
    }

    @Override
    public Set<Long> findUserIdsByRoleId(Long roleId) {
        return new LinkedHashSet<>(jdbc.queryForList("""
                SELECT user_id
                FROM user_roles
                WHERE role_id = ?
                """, Long.class, roleId));
    }

    @Override
    public int countUsersWithPermissionExceptRole(Long roleId, String permissionSlug) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(DISTINCT ur.user_id)
                FROM user_roles ur
                JOIN roles r ON r.id = ur.role_id
                JOIN role_permissions rp ON rp.role_id = r.id
                JOIN permissions p ON p.id = rp.permission_id
                WHERE ur.role_id <> ?
                  AND r.active = TRUE
                  AND r.deleted_at IS NULL
                  AND p.slug = ?
                """, Integer.class, roleId, permissionSlug);
        return count == null ? 0 : count;
    }

    @Override
    public int countUsersWithPermissionExceptUser(Long userId, String permissionSlug) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(DISTINCT ur.user_id)
                FROM user_roles ur
                JOIN roles r ON r.id = ur.role_id
                JOIN role_permissions rp ON rp.role_id = r.id
                JOIN permissions p ON p.id = rp.permission_id
                WHERE ur.user_id <> ?
                  AND r.active = TRUE
                  AND r.deleted_at IS NULL
                  AND p.slug = ?
                """, Integer.class, userId, permissionSlug);
        return count == null ? 0 : count;
    }

    @Override
    public boolean userHasPermissionOutsideRole(Long userId, Long roleId, String permissionSlug) {
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM user_roles ur
                JOIN roles r ON r.id = ur.role_id
                JOIN role_permissions rp ON rp.role_id = r.id
                JOIN permissions p ON p.id = rp.permission_id
                WHERE ur.user_id = ?
                  AND ur.role_id <> ?
                  AND r.active = TRUE
                  AND r.deleted_at IS NULL
                  AND p.slug = ?
                """, Integer.class, userId, roleId, permissionSlug);
        return count != null && count > 0;
    }

    @Override
    public Set<String> findPermissionSlugsByRoleSlugs(Set<String> roleSlugs) {
        Set<String> permissions = new LinkedHashSet<>();
        for (String roleSlug : roleSlugs) {
            permissions.addAll(jdbc.queryForList("""
                    SELECT p.slug
                    FROM roles r
                    JOIN role_permissions rp ON rp.role_id = r.id
                    JOIN permissions p ON p.id = rp.permission_id
                    WHERE r.slug = ?
                      AND r.active = TRUE
                      AND r.deleted_at IS NULL
                      AND p.deleted_at IS NULL
                    """, String.class, roleSlug));
        }
        return permissions;
    }

    private RoleData toRoleData(Long id, String name, String slug, String description, boolean active, boolean systemRole) {
        return new RoleData(id, name, slug, description, active, systemRole, rolePermissions(id));
    }

    private UserAccessData toUserAccessData(Long id, String name, String email, String status) {
        return new UserAccessData(id, name, email, status, userRoles(id), userPermissions(id));
    }

    private Set<String> rolePermissions(Long roleId) {
        return new LinkedHashSet<>(jdbc.queryForList("""
                SELECT p.slug
                FROM role_permissions rp
                JOIN permissions p ON p.id = rp.permission_id
                WHERE rp.role_id = ?
                  AND p.deleted_at IS NULL
                ORDER BY p.slug
                """, String.class, roleId));
    }

    private Set<String> userRoles(Long userId) {
        return new LinkedHashSet<>(jdbc.queryForList("""
                SELECT r.slug
                FROM user_roles ur
                JOIN roles r ON r.id = ur.role_id
                WHERE ur.user_id = ?
                  AND r.deleted_at IS NULL
                ORDER BY r.slug
                """, String.class, userId));
    }

    private Set<String> userPermissions(Long userId) {
        return new LinkedHashSet<>(jdbc.queryForList("""
                SELECT DISTINCT p.slug
                FROM user_roles ur
                JOIN roles r ON r.id = ur.role_id
                JOIN role_permissions rp ON rp.role_id = r.id
                JOIN permissions p ON p.id = rp.permission_id
                WHERE ur.user_id = ?
                  AND r.active = TRUE
                  AND r.deleted_at IS NULL
                  AND p.deleted_at IS NULL
                ORDER BY p.slug
                """, String.class, userId));
    }
}
