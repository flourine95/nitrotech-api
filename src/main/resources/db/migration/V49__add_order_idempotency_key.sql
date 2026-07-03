ALTER TABLE orders
    ADD COLUMN idempotency_key VARCHAR(120);

CREATE UNIQUE INDEX uq_orders_user_idempotency_key
    ON orders (user_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;
