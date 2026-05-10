CREATE TABLE ai_generations (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT NOT NULL DEFAULT 1,
    course_id     BIGINT,
    lecture_id    BIGINT,
    topic_id      BIGINT,
    purpose       VARCHAR(50) NOT NULL,
    request_json  JSONB,
    response_json JSONB,
    model         VARCHAR(50),
    status        VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
    latency_ms    BIGINT,
    error_message TEXT,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_aigen_status  CHECK (status  IN ('SUCCESS','FAILED','PARSE_FAILED')),
    CONSTRAINT chk_aigen_purpose CHECK (purpose IN (
        'GENERATE_LECTURE_NOTES','EXPLAIN_AGAIN','TUTOR_CHAT',
        'TEACH_BACK_ANALYSIS','SESSION_SUMMARY','TOPIC_NOTE_MERGE','WEAK_AREA_UPDATE'
    ))
);

CREATE INDEX idx_aigen_user_time ON ai_generations(user_id, created_at);
CREATE INDEX idx_aigen_purpose   ON ai_generations(user_id, purpose,    created_at);
CREATE INDEX idx_aigen_lecture   ON ai_generations(user_id, lecture_id, created_at);
