WITH permission_slug_map(old_slug, new_slug) AS (
    VALUES
        ('user:view', 'USER_READ'),
        ('user:create', 'USER_MANAGE_ROLE'),
        ('user:update', 'USER_MANAGE_ROLE'),
        ('user:delete', 'USER_MANAGE_ROLE'),
        ('role:view', 'ROLE_READ'),
        ('role:manage', 'ROLE_MANAGE'),
        ('product:view', 'PRODUCT_READ'),
        ('product:create', 'PRODUCT_CREATE'),
        ('product:update', 'PRODUCT_UPDATE'),
        ('product:delete', 'PRODUCT_DELETE'),
        ('category:view', 'CATEGORY_READ'),
        ('category:manage', 'CATEGORY_CREATE'),
        ('category:manage', 'CATEGORY_UPDATE'),
        ('category:manage', 'CATEGORY_DELETE'),
        ('brand:view', 'BRAND_READ'),
        ('brand:manage', 'BRAND_CREATE'),
        ('brand:manage', 'BRAND_UPDATE'),
        ('brand:manage', 'BRAND_DELETE'),
        ('order:view', 'ORDER_READ_ALL'),
        ('order:update', 'ORDER_UPDATE_STATUS'),
        ('order:cancel', 'ORDER_CANCEL_OWN'),
        ('inventory:view', 'INVENTORY_MANAGE'),
        ('inventory:manage', 'INVENTORY_MANAGE'),
        ('promotion:view', 'PROMOTION_MANAGE'),
        ('promotion:manage', 'PROMOTION_MANAGE'),
        ('review:manage', 'REVIEW_MANAGE'),
        ('banner:manage', 'BANNER_MANAGE')
)
INSERT INTO role_permissions (role_id, permission_id)
SELECT DISTINCT rp.role_id, new_permission.id
FROM role_permissions rp
JOIN permissions old_permission ON old_permission.id = rp.permission_id
JOIN permission_slug_map m ON m.old_slug = old_permission.slug
JOIN permissions new_permission ON new_permission.slug = m.new_slug
ON CONFLICT DO NOTHING;

DELETE FROM role_permissions rp
USING permissions p
WHERE p.id = rp.permission_id
  AND p.slug IN (
      'user:view', 'user:create', 'user:update', 'user:delete',
      'role:view', 'role:manage',
      'product:view', 'product:create', 'product:update', 'product:delete',
      'category:view', 'category:manage',
      'brand:view', 'brand:manage',
      'order:view', 'order:update', 'order:cancel',
      'inventory:view', 'inventory:manage',
      'promotion:view', 'promotion:manage',
      'review:manage',
      'banner:manage'
  );

UPDATE permissions
SET deleted_at = NOW(),
    updated_at = NOW()
WHERE deleted_at IS NULL
  AND slug IN (
      'user:view', 'user:create', 'user:update', 'user:delete',
      'role:view', 'role:manage',
      'product:view', 'product:create', 'product:update', 'product:delete',
      'category:view', 'category:manage',
      'brand:view', 'brand:manage',
      'order:view', 'order:update', 'order:cancel',
      'inventory:view', 'inventory:manage',
      'promotion:view', 'promotion:manage',
      'review:manage',
      'banner:manage'
  );
