CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    correlation_id VARCHAR(100) NOT NULL,
    actor_type VARCHAR(30) NOT NULL,
    actor_id BIGINT,
    actor_email VARCHAR(255),
    actor_roles JSONB NOT NULL DEFAULT '[]'::jsonb,
    action VARCHAR(100) NOT NULL,
    resource_type VARCHAR(80) NOT NULL,
    resource_id VARCHAR(100),
    outcome VARCHAR(30) NOT NULL,
    before_data JSONB,
    after_data JSONB,
    metadata JSONB,
    ip_address VARCHAR(100),
    user_agent TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_logs_resource ON audit_logs(resource_type, resource_id);
CREATE INDEX idx_audit_logs_actor ON audit_logs(actor_id);
CREATE INDEX idx_audit_logs_correlation ON audit_logs(correlation_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);
