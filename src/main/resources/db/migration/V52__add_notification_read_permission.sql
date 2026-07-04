INSERT INTO permissions (name, slug, group_name, description, system_permission)
VALUES ('Read Notifications', 'NOTIFICATION_READ', 'notification', 'Read admin notifications', TRUE)
ON CONFLICT (slug) DO UPDATE
SET name = EXCLUDED.name,
    group_name = EXCLUDED.group_name,
    description = EXCLUDED.description,
    system_permission = TRUE;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.slug = 'NOTIFICATION_READ'
WHERE r.slug IN ('admin', 'super_admin', 'staff')
ON CONFLICT DO NOTHING;
