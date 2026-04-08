-- Bỏ unique constraint cũ trên slug
ALTER TABLE categories DROP CONSTRAINT IF EXISTS categories_slug_key;

-- Thêm partial unique index: slug chỉ unique trong active records
CREATE UNIQUE INDEX idx_categories_slug_active ON categories(slug) WHERE deleted_at IS NULL;
