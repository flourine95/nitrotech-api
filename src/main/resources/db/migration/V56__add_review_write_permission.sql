INSERT INTO permissions (name, slug, group_name, description, system_permission)
VALUES ('Write Reviews', 'REVIEW_WRITE', 'review', 'Create, update, delete, and report reviews', TRUE)
ON CONFLICT (slug) DO UPDATE
SET name = EXCLUDED.name,
    group_name = EXCLUDED.group_name,
    description = EXCLUDED.description,
    system_permission = TRUE;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.slug = 'REVIEW_WRITE'
WHERE r.slug IN ('customer', 'admin', 'super_admin')
ON CONFLICT DO NOTHING;
