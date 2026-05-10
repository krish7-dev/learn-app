CREATE TABLE lectures (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL DEFAULT 1,
    course_id       BIGINT NOT NULL REFERENCES courses(id),
    module_name     VARCHAR(255),
    title           VARCHAR(255) NOT NULL,
    source_name     VARCHAR(100),
    source_order    INT,
    raw_content     TEXT,
    status          VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED',
    difficulty      VARCHAR(10),
    last_studied_at TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMP,
    CONSTRAINT chk_lecture_status    CHECK (status IN ('NOT_STARTED','IN_PROGRESS','COMPLETED')),
    CONSTRAINT chk_lecture_difficulty CHECK (difficulty IS NULL OR difficulty IN ('EASY','MEDIUM','HARD'))
);

CREATE INDEX idx_lectures_user_course   ON lectures(user_id, course_id)     WHERE deleted_at IS NULL;
CREATE INDEX idx_lectures_user_status   ON lectures(user_id, status)         WHERE deleted_at IS NULL;
CREATE INDEX idx_lectures_last_studied  ON lectures(user_id, last_studied_at) WHERE deleted_at IS NULL;
