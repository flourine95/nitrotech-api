CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    href VARCHAR(500),
    recipient_user_id BIGINT,
    required_authority VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_notifications_recipient_user
        FOREIGN KEY (recipient_user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_notifications_audience
        CHECK (recipient_user_id IS NOT NULL OR required_authority IS NOT NULL)
);

CREATE TABLE notification_reads (
    notification_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    read_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (notification_id, user_id),
    CONSTRAINT fk_notification_reads_notification
        FOREIGN KEY (notification_id) REFERENCES notifications(id) ON DELETE CASCADE,
    CONSTRAINT fk_notification_reads_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_notifications_created_at ON notifications (created_at DESC);
CREATE INDEX idx_notifications_recipient_user ON notifications (recipient_user_id, created_at DESC);
CREATE INDEX idx_notifications_required_authority ON notifications (required_authority, created_at DESC);
