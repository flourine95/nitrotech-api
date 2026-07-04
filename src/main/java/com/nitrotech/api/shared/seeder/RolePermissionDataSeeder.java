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
                new PermissionSeed("Read Users", "USER_READ", "user", "Read users"),
                new PermissionSeed("Create User", "USER_CREATE", "user", "Create users"),
                new PermissionSeed("Update User", "USER_UPDATE", "user", "Update users"),
                new PermissionSeed("Delete User", "USER_DELETE", "user", "Delete users"),
                new PermissionSeed("Manage User Roles", "USER_MANAGE_ROLE", "user", "Assign roles to users"),
                new PermissionSeed("Read Roles", "ROLE_READ", "role", "Read roles and permissions"),
                new PermissionSeed("Manage Roles", "ROLE_MANAGE", "role", "Create, update, delete roles and assign permissions"),
                new PermissionSeed("Read Products", "PRODUCT_READ", "product", "Read products"),
                new PermissionSeed("Create Product", "PRODUCT_CREATE", "product", "Create products"),
                new PermissionSeed("Update Product", "PRODUCT_UPDATE", "product", "Update products"),
                new PermissionSeed("Delete Product", "PRODUCT_DELETE", "product", "Delete products"),
                new PermissionSeed("Read Categories", "CATEGORY_READ", "category", "Read categories"),
                new PermissionSeed("Create Category", "CATEGORY_CREATE", "category", "Create categories"),
                new PermissionSeed("Update Category", "CATEGORY_UPDATE", "category", "Update categories"),
                new PermissionSeed("Delete Category", "CATEGORY_DELETE", "category", "Delete categories"),
                new PermissionSeed("Read Brands", "BRAND_READ", "brand", "Read brands"),
                new PermissionSeed("Create Brand", "BRAND_CREATE", "brand", "Create brands"),
                new PermissionSeed("Update Brand", "BRAND_UPDATE", "brand", "Update brands"),
                new PermissionSeed("Delete Brand", "BRAND_DELETE", "brand", "Delete brands"),
                new PermissionSeed("Read Own Orders", "ORDER_READ_OWN", "order", "Read own orders"),
                new PermissionSeed("Read All Orders", "ORDER_READ_ALL", "order", "Read all orders"),
                new PermissionSeed("Update Order Status", "ORDER_UPDATE_STATUS", "order", "Update order status"),
                new PermissionSeed("Cancel Own Order", "ORDER_CANCEL_OWN", "order", "Cancel own orders"),
                new PermissionSeed("Manage Inventory", "INVENTORY_MANAGE", "inventory", "Manage inventory"),
                new PermissionSeed("Manage Promotions", "PROMOTION_MANAGE", "promotion", "Manage promotions"),
                new PermissionSeed("Manage Reviews", "REVIEW_MANAGE", "review", "Manage reviews"),
                new PermissionSeed("Manage Banners", "BANNER_MANAGE", "banner", "Manage banners"),
                new PermissionSeed("Manage Media", "MEDIA_MANAGE", "media", "Upload and browse media assets"),
                new PermissionSeed("Read Notifications", "NOTIFICATION_READ", "notification", "Read admin notifications")
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
                    'PRODUCT_READ', 'PRODUCT_CREATE', 'PRODUCT_UPDATE',
                    'CATEGORY_READ', 'BRAND_READ',
                    'ORDER_READ_ALL', 'ORDER_UPDATE_STATUS',
                    'INVENTORY_MANAGE', 'REVIEW_MANAGE',
                    'PROMOTION_MANAGE', 'MEDIA_MANAGE',
                    'NOTIFICATION_READ'
                )
                WHERE r.slug = 'staff'
                ON CONFLICT DO NOTHING
                """);

        log.info("Seeded {} roles and {} permissions", roles.size(), permissions.size());
    }

    private record RoleSeed(String name, String slug, String description) {}

    private record PermissionSeed(String name, String slug, String groupName, String description) {}
}
