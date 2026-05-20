-- Add manual badge fields for admin-set badges with expiry support
ALTER TABLE products 
ADD COLUMN manual_badge VARCHAR(50),
ADD COLUMN manual_badge_expires_at TIMESTAMP;

-- Comments
COMMENT ON COLUMN products.manual_badge IS 
  'Admin-set badge (featured, exclusive, flashsale, etc). Overrides auto-computed badge if not expired. NULL means use auto-computed badge.';

COMMENT ON COLUMN products.manual_badge_expires_at IS 
  'Expiry time for manual badge. NULL = never expire. After expiry, fallback to auto-computed badge.';

-- Example values: 'featured', 'exclusive', 'flashsale', 'limited', 'preorder', etc.
