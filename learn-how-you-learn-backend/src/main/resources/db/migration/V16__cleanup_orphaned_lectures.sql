UPDATE lectures
SET deleted_at = NOW()
WHERE deleted_at IS NULL
  AND course_id IN (SELECT id FROM courses WHERE deleted_at IS NOT NULL);
