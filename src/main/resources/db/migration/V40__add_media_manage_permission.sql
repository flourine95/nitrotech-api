INSERT INTO permissions (name, slug, group_name, description, system_permission)
VALUES ('Manage Media', 'MEDIA_MANAGE', 'media', 'Upload and browse media assets', TRUE)
ON CONFLICT (slug) DO UPDATE
SET name = EXCLUDED.name,
    group_name = EXCLUDED.group_name,
    description = EXCLUDED.description,
    system_permission = TRUE,
    deleted_at = NULL,
    updated_at = NOW();

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.slug = 'MEDIA_MANAGE'
WHERE r.slug IN ('admin', 'super_admin', 'staff')
ON CONFLICT DO NOTHING;
