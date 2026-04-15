ALTER TABLE categories ADD COLUMN sort_order INT NOT NULL DEFAULT 0;

-- Set sort_order ban đầu theo thứ tự id trong cùng parent
UPDATE categories c
SET sort_order = sub.rn - 1
FROM (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY COALESCE(parent_id, -1) ORDER BY id) AS rn
    FROM categories
) sub
WHERE c.id = sub.id;
