ALTER TABLE orders ADD COLUMN order_code VARCHAR(50);

-- Update existing orders with order codes matching the SO-000 format
UPDATE orders SET order_code = 'SO-' || LPAD(CAST(id AS VARCHAR), 3, '0');

-- Apply NOT NULL, UNIQUE constraints, and create index for fast searches
ALTER TABLE orders ALTER COLUMN order_code SET NOT NULL;
ALTER TABLE orders ADD CONSTRAINT uk_orders_order_code UNIQUE (order_code);
CREATE INDEX idx_orders_order_code ON orders (order_code);
