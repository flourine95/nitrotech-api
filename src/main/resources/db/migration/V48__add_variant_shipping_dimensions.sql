ALTER TABLE product_variants
    ADD COLUMN weight_grams INTEGER,
    ADD COLUMN length_cm DECIMAL(8, 2),
    ADD COLUMN width_cm DECIMAL(8, 2),
    ADD COLUMN height_cm DECIMAL(8, 2);

ALTER TABLE order_items
    ADD COLUMN weight_grams INTEGER,
    ADD COLUMN length_cm DECIMAL(8, 2),
    ADD COLUMN width_cm DECIMAL(8, 2),
    ADD COLUMN height_cm DECIMAL(8, 2);
