CREATE TABLE inventories (
    id                  BIGSERIAL PRIMARY KEY,
    variant_id          BIGINT    NOT NULL UNIQUE,
    quantity            INT       NOT NULL DEFAULT 0,
    low_stock_threshold INT       NOT NULL DEFAULT 5,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_inventory_variant FOREIGN KEY (variant_id) REFERENCES product_variants(id) ON DELETE CASCADE
);
