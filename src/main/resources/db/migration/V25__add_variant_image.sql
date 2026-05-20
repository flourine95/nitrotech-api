-- Add image reference to product variants
ALTER TABLE product_variants 
ADD COLUMN image_id BIGINT;

-- Foreign key with ON DELETE SET NULL (safe deletion)
ALTER TABLE product_variants 
ADD CONSTRAINT fk_variant_image 
  FOREIGN KEY (image_id) 
  REFERENCES product_images(id) 
  ON DELETE SET NULL;

-- Index for performance
CREATE INDEX idx_variant_image ON product_variants(image_id);

-- Comment
COMMENT ON COLUMN product_variants.image_id IS 
  'Reference to product_images.id - main image for this variant. NULL means use product thumbnail.';
