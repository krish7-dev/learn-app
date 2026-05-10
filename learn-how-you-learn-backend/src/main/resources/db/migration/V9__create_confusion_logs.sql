CREATE TABLE confusion_logs (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT NOT NULL DEFAULT 1,
    course_id      BIGINT,
    lecture_id     BIGINT,
    topic_id       BIGINT,
    section_title  VARCHAR(255),
    confusion_type VARCHAR(30) NOT NULL,
    note           TEXT,
    resolved       BOOLEAN NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_confusion_type CHECK (confusion_type IN (
        'THEORY_UNCLEAR','EXAMPLE_UNCLEAR','WHEN_TO_USE_UNCLEAR',
        'CODE_CONFUSING','EDGE_CASE_CONFUSING','LOST_FOCUS'
    ))
);

CREATE INDEX idx_confusion_lecture ON confusion_logs(user_id, lecture_id, created_at);
CREATE INDEX idx_confusion_topic   ON confusion_logs(user_id, topic_id,   created_at);
