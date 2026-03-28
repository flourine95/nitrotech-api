CREATE TABLE orders (
    id               BIGSERIAL      PRIMARY KEY,
    user_id          BIGINT         NOT NULL,
    shipping_address JSONB          NOT NULL,
    status           VARCHAR(20)    NOT NULL DEFAULT 'pending',
    payment_method   VARCHAR(20)    NOT NULL DEFAULT 'cod',
    total_amount     DECIMAL(15, 2) NOT NULL,
    discount_amount  DECIMAL(15, 2) NOT NULL DEFAULT 0,
    shipping_fee     DECIMAL(15, 2) NOT NULL DEFAULT 0,
    final_amount     DECIMAL(15, 2) NOT NULL,
    promotion_code   VARCHAR(50),
    note             TEXT,
    created_at       TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP      NOT NULL DEFAULT NOW(),
    deleted_at       TIMESTAMP,
    CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_orders_status  ON orders (status);
CREATE INDEX idx_orders_user_id ON orders (user_id);

CREATE TABLE order_items (
    id         BIGSERIAL      PRIMARY KEY,
    order_id   BIGINT         NOT NULL,
    variant_id BIGINT         NOT NULL,
    name       VARCHAR(255)   NOT NULL,
    sku        VARCHAR(100)   NOT NULL,
    quantity   INT            NOT NULL,
    unit_price DECIMAL(15, 2) NOT NULL,
    subtotal   DECIMAL(15, 2) NOT NULL,
    created_at TIMESTAMP      NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_item_order   FOREIGN KEY (order_id)   REFERENCES orders(id)           ON DELETE CASCADE,
    CONSTRAINT fk_item_variant FOREIGN KEY (variant_id) REFERENCES product_variants(id)
);
