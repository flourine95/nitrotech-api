CREATE INDEX idx_reviews_product_status_created
    ON reviews (product_id, status, created_at DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_reviews_user_order
    ON reviews (user_id, order_id)
    WHERE deleted_at IS NULL;

CREATE TABLE review_reports (
    id          BIGSERIAL   PRIMARY KEY,
    review_id   BIGINT      NOT NULL,
    user_id     BIGINT      NOT NULL,
    reason      VARCHAR(500) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_review_report_review FOREIGN KEY (review_id) REFERENCES reviews(id),
    CONSTRAINT fk_review_report_user FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE (review_id, user_id)
);
