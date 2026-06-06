ALTER TABLE lecture_topics DROP CONSTRAINT chk_lt_importance;
ALTER TABLE lecture_topics ADD CONSTRAINT chk_lt_importance CHECK (importance IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL'));
