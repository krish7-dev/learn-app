CREATE TABLE learning_targets (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL DEFAULT 1,
    course_id       BIGINT REFERENCES courses(id),
    module_name     VARCHAR(255),
    topic_id        BIGINT REFERENCES topics(id),
    title           VARCHAR(255) NOT NULL,
    description     TEXT,
    target_scope    VARCHAR(20) NOT NULL DEFAULT 'COURSE',
    target_date     DATE NOT NULL,
    daily_minutes   INT NOT NULL DEFAULT 60,
    weekly_minutes  INT,
    priority        VARCHAR(10) NOT NULL DEFAULT 'MEDIUM',
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_target_scope    CHECK (target_scope IN ('COURSE','MODULE','TOPIC','CUSTOM')),
    CONSTRAINT chk_target_status   CHECK (status IN ('ACTIVE','COMPLETED','PAUSED','ABANDONED')),
    CONSTRAINT chk_target_priority CHECK (priority IN ('LOW','MEDIUM','HIGH'))
);

CREATE INDEX idx_targets_user_status   ON learning_targets(user_id, status);
CREATE INDEX idx_targets_user_priority ON learning_targets(user_id, priority, status);
