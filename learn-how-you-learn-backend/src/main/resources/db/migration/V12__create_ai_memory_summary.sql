CREATE TABLE ai_memory_summary (
    id                     BIGSERIAL PRIMARY KEY,
    user_id                BIGINT NOT NULL DEFAULT 1,
    course_id              BIGINT,
    lecture_id             BIGINT,
    topic_id               BIGINT,
    summary                TEXT,
    understood_points      JSONB,
    struggled_points       JSONB,
    recommended_next_steps JSONB,
    profile_signals        JSONB,
    created_at             TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_memory_lecture ON ai_memory_summary(user_id, lecture_id, created_at);
CREATE INDEX idx_memory_topic   ON ai_memory_summary(user_id, topic_id,   created_at);
