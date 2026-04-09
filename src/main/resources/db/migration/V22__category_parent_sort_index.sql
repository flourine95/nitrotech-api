CREATE INDEX idx_categories_parent_sort ON categories(parent_id, sort_order) WHERE deleted_at IS NULL;
