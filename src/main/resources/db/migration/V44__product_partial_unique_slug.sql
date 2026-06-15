ALTER TABLE products DROP CONSTRAINT IF EXISTS products_slug_key;

CREATE UNIQUE INDEX idx_products_slug_active ON products(slug) WHERE deleted_at IS NULL;
