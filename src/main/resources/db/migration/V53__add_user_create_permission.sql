INSERT INTO permissions (name, slug, group_name, description, system_permission)
VALUES ('Create User', 'USER_CREATE', 'user', 'Create users', TRUE)
ON CONFLICT (slug) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.slug = 'USER_CREATE'
WHERE r.slug = 'admin'
ON CONFLICT DO NOTHING;
