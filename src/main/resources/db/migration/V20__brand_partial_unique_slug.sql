ALTER TABLE brands DROP CONSTRAINT IF EXISTS brands_slug_key;

CREATE UNIQUE INDEX idx_brands_slug_active ON brands(slug) WHERE deleted_at IS NULL;
