CREATE TABLE topics (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL DEFAULT 1,
    name            VARCHAR(255) NOT NULL,
    normalized_name VARCHAR(255) NOT NULL,
    description     TEXT,
    category        VARCHAR(100),
    difficulty      VARCHAR(10),
    mastery_score   INT NOT NULL DEFAULT 0,
    status          VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED',
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_topic_status     CHECK (status IN ('NOT_STARTED','LEARNING','REVISING','MASTERED')),
    CONSTRAINT chk_topic_difficulty CHECK (difficulty IS NULL OR difficulty IN ('EASY','MEDIUM','HARD'))
);

CREATE UNIQUE INDEX idx_topics_user_normalized ON topics(user_id, normalized_name);
CREATE INDEX        idx_topics_user_status     ON topics(user_id, status);
CREATE INDEX        idx_topics_user_mastery    ON topics(user_id, mastery_score);
