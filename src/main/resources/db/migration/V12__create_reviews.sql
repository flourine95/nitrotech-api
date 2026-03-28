CREATE TABLE reviews (
    id         BIGSERIAL   PRIMARY KEY,
    product_id BIGINT      NOT NULL,
    user_id    BIGINT      NOT NULL,
    order_id   BIGINT      NOT NULL,
    rating     SMALLINT    NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment    TEXT,
    images     JSONB,
    status     VARCHAR(20) NOT NULL DEFAULT 'pending',
    created_at TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP   NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP,
    UNIQUE (product_id, user_id, order_id),
    CONSTRAINT fk_review_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_review_user    FOREIGN KEY (user_id)    REFERENCES users(id),
    CONSTRAINT fk_review_order   FOREIGN KEY (order_id)   REFERENCES orders(id)
);
