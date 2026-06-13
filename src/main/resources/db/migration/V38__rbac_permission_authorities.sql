ALTER TABLE roles
    ADD COLUMN IF NOT EXISTS system_role BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE permissions
    ADD COLUMN IF NOT EXISTS system_permission BOOLEAN NOT NULL DEFAULT FALSE;

INSERT INTO roles (name, slug, description, system_role)
VALUES
    ('Super Admin', 'super_admin', 'Unrestricted system administrator', TRUE),
    ('Admin', 'admin', 'Full system access', TRUE),
    ('Staff', 'staff', 'Operations staff', TRUE),
    ('Customer', 'customer', 'Customer account', TRUE)
ON CONFLICT (slug) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description,
    system_role = TRUE;

INSERT INTO permissions (name, slug, group_name, description, system_permission)
VALUES
    ('Read Own Orders', 'ORDER_READ_OWN', 'order', 'Read own orders', TRUE),
    ('Read All Orders', 'ORDER_READ_ALL', 'order', 'Read all orders', TRUE),
    ('Update Order Status', 'ORDER_UPDATE_STATUS', 'order', 'Update order status', TRUE),
    ('Cancel Own Order', 'ORDER_CANCEL_OWN', 'order', 'Cancel own orders', TRUE),
    ('Read Shipments', 'SHIPMENT_READ', 'shipment', 'Read shipments', TRUE),
    ('Create Shipment', 'SHIPMENT_CREATE', 'shipment', 'Create shipments with carriers', TRUE),
    ('Update Shipment', 'SHIPMENT_UPDATE', 'shipment', 'Update shipment data', TRUE),
    ('Cancel Shipment', 'SHIPMENT_CANCEL', 'shipment', 'Cancel shipments', TRUE),
    ('Read Products', 'PRODUCT_READ', 'product', 'Read products', TRUE),
    ('Create Product', 'PRODUCT_CREATE', 'product', 'Create products', TRUE),
    ('Update Product', 'PRODUCT_UPDATE', 'product', 'Update products', TRUE),
    ('Delete Product', 'PRODUCT_DELETE', 'product', 'Delete products', TRUE),
    ('Read Categories', 'CATEGORY_READ', 'category', 'Read categories', TRUE),
    ('Create Category', 'CATEGORY_CREATE', 'category', 'Create categories', TRUE),
    ('Update Category', 'CATEGORY_UPDATE', 'category', 'Update categories', TRUE),
    ('Delete Category', 'CATEGORY_DELETE', 'category', 'Delete categories', TRUE),
    ('Read Brands', 'BRAND_READ', 'brand', 'Read brands', TRUE),
    ('Create Brand', 'BRAND_CREATE', 'brand', 'Create brands', TRUE),
    ('Update Brand', 'BRAND_UPDATE', 'brand', 'Update brands', TRUE),
    ('Delete Brand', 'BRAND_DELETE', 'brand', 'Delete brands', TRUE),
    ('Read Users', 'USER_READ', 'user', 'Read users', TRUE),
    ('Manage User Roles', 'USER_MANAGE_ROLE', 'user', 'Assign roles to users', TRUE),
    ('Read Roles', 'ROLE_READ', 'role', 'Read roles and permissions', TRUE),
    ('Manage Roles', 'ROLE_MANAGE', 'role', 'Create, update, delete roles and assign permissions', TRUE),
    ('Manage Banners', 'BANNER_MANAGE', 'banner', 'Manage banners', TRUE),
    ('Manage Inventory', 'INVENTORY_MANAGE', 'inventory', 'Manage inventory', TRUE),
    ('Manage Promotions', 'PROMOTION_MANAGE', 'promotion', 'Manage promotions', TRUE),
    ('Manage Reviews', 'REVIEW_MANAGE', 'review', 'Manage reviews', TRUE)
ON CONFLICT (slug) DO UPDATE
SET name = EXCLUDED.name,
    group_name = EXCLUDED.group_name,
    description = EXCLUDED.description,
    system_permission = TRUE;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.slug IN ('admin', 'super_admin')
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.slug IN (
    'ORDER_READ_ALL', 'ORDER_UPDATE_STATUS',
    'SHIPMENT_READ', 'SHIPMENT_CREATE', 'SHIPMENT_UPDATE',
    'PRODUCT_READ', 'PRODUCT_CREATE', 'PRODUCT_UPDATE',
    'CATEGORY_READ', 'BRAND_READ',
    'INVENTORY_MANAGE', 'PROMOTION_MANAGE', 'REVIEW_MANAGE'
)
WHERE r.slug = 'staff'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.slug IN (
    'ORDER_READ_OWN', 'ORDER_CANCEL_OWN',
    'PRODUCT_READ', 'CATEGORY_READ', 'BRAND_READ'
)
WHERE r.slug = 'customer'
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.slug = 'admin'
WHERE u.email IN ('admin@gmail.com', 'flourinee@gmail.com')
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.slug = 'customer'
WHERE NOT EXISTS (
    SELECT 1
    FROM user_roles ur
    WHERE ur.user_id = u.id
)
ON CONFLICT DO NOTHING;
