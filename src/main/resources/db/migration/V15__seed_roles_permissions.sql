-- Roles
INSERT INTO roles (name, slug, description) VALUES
    ('Admin',    'admin',    'Full system access'),
    ('Staff',    'staff',    'Manage products, orders, inventory'),
    ('Customer', 'customer', 'Browse and purchase products');

-- Permissions by group
INSERT INTO permissions (name, slug, group_name, description) VALUES
    -- user
    ('View Users',   'user:view',   'user', 'View user list and details'),
    ('Create User',  'user:create', 'user', 'Create new users'),
    ('Update User',  'user:update', 'user', 'Update user information'),
    ('Delete User',  'user:delete', 'user', 'Delete users'),
    -- role
    ('View Roles',   'role:view',   'role', 'View roles and permissions'),
    ('Manage Roles', 'role:manage', 'role', 'Create, update, delete roles and assign permissions'),
    -- product
    ('View Products',   'product:view',   'product', 'View product list and details'),
    ('Create Product',  'product:create', 'product', 'Create new products'),
    ('Update Product',  'product:update', 'product', 'Update product information'),
    ('Delete Product',  'product:delete', 'product', 'Delete products'),
    -- category
    ('View Categories',  'category:view',   'category', 'View categories'),
    ('Manage Categories','category:manage', 'category', 'Create, update, delete categories'),
    -- brand
    ('View Brands',  'brand:view',   'brand', 'View brands'),
    ('Manage Brands','brand:manage', 'brand', 'Create, update, delete brands'),
    -- order
    ('View Orders',   'order:view',   'order', 'View all orders'),
    ('Update Order',  'order:update', 'order', 'Update order status'),
    ('Cancel Order',  'order:cancel', 'order', 'Cancel orders'),
    -- inventory
    ('View Inventory',   'inventory:view',   'inventory', 'View inventory levels'),
    ('Manage Inventory', 'inventory:manage', 'inventory', 'Adjust inventory quantities'),
    -- promotion
    ('View Promotions',   'promotion:view',   'promotion', 'View promotions'),
    ('Manage Promotions', 'promotion:manage', 'promotion', 'Create, update, delete promotions'),
    -- review
    ('Manage Reviews', 'review:manage', 'review', 'Approve or reject reviews'),
    -- banner
    ('Manage Banners', 'banner:manage', 'banner', 'Create, update, delete banners');

-- Admin gets all permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.slug = 'admin';

-- Staff gets product, order, inventory, review permissions
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
WHERE r.slug = 'staff';
