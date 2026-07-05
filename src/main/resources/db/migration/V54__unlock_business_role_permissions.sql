UPDATE roles
SET system_role = FALSE,
    updated_at = NOW()
WHERE slug IN ('staff', 'customer')
  AND system_role = TRUE;
