CREATE TABLE lecture_chat_messages (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL DEFAULT 1,
    course_id  BIGINT,
    lecture_id BIGINT NOT NULL,
    role       VARCHAR(20) NOT NULL,
    message    TEXT NOT NULL,
    metadata   JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_chat_role CHECK (role IN ('USER','ASSISTANT','SYSTEM'))
);

CREATE INDEX idx_chat_lecture ON lecture_chat_messages(user_id, lecture_id, created_at);
