CREATE TABLE learning_events (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL DEFAULT 1,
    course_id  BIGINT,
    lecture_id BIGINT,
    topic_id   BIGINT,
    event_type VARCHAR(50) NOT NULL,
    payload    JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_event_type CHECK (event_type IN (
        'COURSE_CREATED','LECTURE_CREATED','NOTES_GENERATED','TOPICS_EXTRACTED',
        'CONFUSED_CLICKED','EXPLAIN_AGAIN_REQUESTED',
        'FLASHCARD_FAILED','FLASHCARD_PASSED','QUIZ_WRONG','QUIZ_CORRECT',
        'TEACH_BACK_SUBMITTED','LECTURE_COMPLETED','TOPIC_REVISED',
        'REVISION_DONE','TUTOR_CHAT_MESSAGE'
    ))
);

CREATE INDEX idx_events_user_time ON learning_events(user_id, created_at);
CREATE INDEX idx_events_lecture   ON learning_events(user_id, lecture_id, created_at);
CREATE INDEX idx_events_topic     ON learning_events(user_id, topic_id,   created_at);
CREATE INDEX idx_events_type      ON learning_events(user_id, event_type, created_at);
