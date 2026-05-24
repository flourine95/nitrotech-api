-- Add indexes for product search optimization

-- Index for product name search (case-insensitive)
CREATE INDEX idx_products_name_lower ON products (LOWER(name));

-- Index for active products filter
CREATE INDEX idx_products_active_deleted ON products (active, deleted_at) WHERE deleted_at IS NULL;

-- Index for category slug lookup
CREATE INDEX idx_categories_slug ON categories (slug) WHERE deleted_at IS NULL;

-- Index for brand slug lookup
CREATE INDEX idx_brands_slug ON brands (slug) WHERE deleted_at IS NULL;

-- Index for product variants price aggregation
CREATE INDEX idx_product_variants_product_price ON product_variants (product_id, price) 
    WHERE deleted_at IS NULL AND active = true;

-- Composite index for product search filters
CREATE INDEX idx_products_search ON products (active, deleted_at, category_id, brand_id);
