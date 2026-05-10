CREATE TABLE courses (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL DEFAULT 1,
    title       VARCHAR(255) NOT NULL,
    description TEXT,
    goal        TEXT,
    status      VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED',
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMP,
    CONSTRAINT chk_course_status CHECK (status IN ('NOT_STARTED','IN_PROGRESS','COMPLETED','ARCHIVED'))
);

CREATE INDEX idx_courses_user_id ON courses(user_id) WHERE deleted_at IS NULL;
