CREATE TABLE revision_items (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT NOT NULL DEFAULT 1,
    course_id     BIGINT,
    lecture_id    BIGINT,
    topic_id      BIGINT,
    title         VARCHAR(255) NOT NULL,
    revision_type VARCHAR(20) NOT NULL,
    due_at        TIMESTAMP NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    priority      VARCHAR(10) NOT NULL DEFAULT 'MEDIUM',
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_rev_type     CHECK (revision_type IN ('RECALL','FLASHCARD','PRACTICE','TEACH_BACK','MIXED_TEST')),
    CONSTRAINT chk_rev_status   CHECK (status        IN ('PENDING','DONE','SKIPPED')),
    CONSTRAINT chk_rev_priority CHECK (priority      IN ('LOW','MEDIUM','HIGH'))
);

CREATE INDEX idx_revision_due   ON revision_items(user_id, due_at,    status);
CREATE INDEX idx_revision_topic ON revision_items(user_id, topic_id,  status);
