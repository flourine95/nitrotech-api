package com.nitrotech.api.shared.seeder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@Profile("dev")
@Order(1)
@RequiredArgsConstructor
public class RolePermissionDataSeeder implements CommandLineRunner {

    private final JdbcTemplate jdbc;

    @Override
    @Transactional
    public void run(String... args) {
        Long existingRoles = jdbc.queryForObject("SELECT COUNT(*) FROM roles", Long.class);
        Long existingPermissions = jdbc.queryForObject("SELECT COUNT(*) FROM permissions", Long.class);
        if ((existingRoles != null && existingRoles > 0) || (existingPermissions != null && existingPermissions > 0)) {
            log.info("Roles or permissions already exist, skipping seed");
            return;
        }

        List<RoleSeed> roles = List.of(
                new RoleSeed("Admin", "admin", "Full system access"),
                new RoleSeed("Staff", "staff", "Manage products, orders, inventory"),
                new RoleSeed("Customer", "customer", "Browse and purchase products")
        );

        List<PermissionSeed> permissions = List.of(
                new PermissionSeed("View Users", "user:view", "user", "View user list and details"),
                new PermissionSeed("Create User", "user:create", "user", "Create new users"),
                new PermissionSeed("Update User", "user:update", "user", "Update user information"),
                new PermissionSeed("Delete User", "user:delete", "user", "Delete users"),
                new PermissionSeed("View Roles", "role:view", "role", "View roles and permissions"),
                new PermissionSeed("Manage Roles", "role:manage", "role", "Create, update, delete roles and assign permissions"),
                new PermissionSeed("View Products", "product:view", "product", "View product list and details"),
                new PermissionSeed("Create Product", "product:create", "product", "Create new products"),
                new PermissionSeed("Update Product", "product:update", "product", "Update product information"),
                new PermissionSeed("Delete Product", "product:delete", "product", "Delete products"),
                new PermissionSeed("View Categories", "category:view", "category", "View categories"),
                new PermissionSeed("Manage Categories", "category:manage", "category", "Create, update, delete categories"),
                new PermissionSeed("View Brands", "brand:view", "brand", "View brands"),
                new PermissionSeed("Manage Brands", "brand:manage", "brand", "Create, update, delete brands"),
                new PermissionSeed("View Orders", "order:view", "order", "View all orders"),
                new PermissionSeed("Update Order", "order:update", "order", "Update order status"),
                new PermissionSeed("Cancel Order", "order:cancel", "order", "Cancel orders"),
                new PermissionSeed("View Inventory", "inventory:view", "inventory", "View inventory levels"),
                new PermissionSeed("Manage Inventory", "inventory:manage", "inventory", "Adjust inventory quantities"),
                new PermissionSeed("View Promotions", "promotion:view", "promotion", "View promotions"),
                new PermissionSeed("Manage Promotions", "promotion:manage", "promotion", "Create, update, delete promotions"),
                new PermissionSeed("Manage Reviews", "review:manage", "review", "Approve or reject reviews"),
                new PermissionSeed("Manage Banners", "banner:manage", "banner", "Create, update, delete banners")
        );

        jdbc.batchUpdate("""
                INSERT INTO roles (name, slug, description)
                VALUES (?, ?, ?)
                """, roles, roles.size(), (ps, role) -> {
            ps.setString(1, role.name());
            ps.setString(2, role.slug());
            ps.setString(3, role.description());
        });

        jdbc.batchUpdate("""
                INSERT INTO permissions (name, slug, group_name, description)
                VALUES (?, ?, ?, ?)
                """, permissions, permissions.size(), (ps, permission) -> {
            ps.setString(1, permission.name());
            ps.setString(2, permission.slug());
            ps.setString(3, permission.groupName());
            ps.setString(4, permission.description());
        });

        jdbc.update("""
                INSERT INTO role_permissions (role_id, permission_id)
                SELECT r.id, p.id
                FROM roles r, permissions p
                WHERE r.slug = 'admin'
                ON CONFLICT DO NOTHING
                """);

        jdbc.update("""
                INSERT INTO role_permissions (role_id, permission_id)
                SELECT r.id, p.id
                FROM roles r
                JOIN permissions p ON p.slug IN (
                    'product:view', 'product:create', 'product:update',
                    'category:view', 'brand:view',
                    'order:view', 'order:update',
                    'inventory:view', 'inventory:manage',
                    'review:manage',
                    'promotion:view'
                )
                WHERE r.slug = 'staff'
                ON CONFLICT DO NOTHING
                """);

        log.info("Seeded {} roles and {} permissions", roles.size(), permissions.size());
    }

    private record RoleSeed(String name, String slug, String description) {}

    private record PermissionSeed(String name, String slug, String groupName, String description) {}
}
