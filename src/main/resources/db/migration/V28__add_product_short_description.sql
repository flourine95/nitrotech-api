ALTER TABLE products
ADD COLUMN short_description VARCHAR(500);

UPDATE products
SET short_description = LEFT(regexp_replace(COALESCE(description, ''), '<[^>]*>', '', 'g'), 240)
WHERE description IS NOT NULL
  AND short_description IS NULL;
