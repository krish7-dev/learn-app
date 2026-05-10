CREATE TABLE weak_areas (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL DEFAULT 1,
    course_id  BIGINT,
    lecture_id BIGINT,
    topic_id   BIGINT,
    topic      VARCHAR(255),
    weakness   TEXT NOT NULL,
    severity   VARCHAR(10) NOT NULL DEFAULT 'MEDIUM',
    evidence   TEXT,
    status     VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_weak_severity CHECK (severity IN ('LOW','MEDIUM','HIGH')),
    CONSTRAINT chk_weak_status   CHECK (status   IN ('ACTIVE','IMPROVING','RESOLVED'))
);

CREATE INDEX idx_weak_areas_topic_status   ON weak_areas(user_id, topic_id,   status);
CREATE INDEX idx_weak_areas_lecture_status ON weak_areas(user_id, lecture_id, status);
CREATE INDEX idx_weak_areas_severity       ON weak_areas(user_id, severity,   status);
