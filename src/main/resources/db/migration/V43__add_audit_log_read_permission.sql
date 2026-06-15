INSERT INTO permissions (name, slug, group_name, description, system_permission)
VALUES ('View Audit Logs', 'AUDIT_LOG_READ', 'System', 'View system audit trail', TRUE)
ON CONFLICT (slug) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.slug = 'AUDIT_LOG_READ'
WHERE r.slug IN ('admin', 'super_admin')
ON CONFLICT DO NOTHING;
