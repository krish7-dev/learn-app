CREATE TABLE lecture_topics (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL DEFAULT 1,
    lecture_id      BIGINT NOT NULL REFERENCES lectures(id),
    topic_id        BIGINT NOT NULL REFERENCES topics(id),
    importance      VARCHAR(10) NOT NULL DEFAULT 'MEDIUM',
    coverage_level  VARCHAR(20) NOT NULL DEFAULT 'INTRO',
    evidence        TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_lt_importance CHECK (importance    IN ('LOW','MEDIUM','HIGH')),
    CONSTRAINT chk_lt_coverage   CHECK (coverage_level IN ('INTRO','INTERMEDIATE','ADVANCED'))
);

CREATE UNIQUE INDEX idx_lecture_topics_unique  ON lecture_topics(user_id, lecture_id, topic_id);
CREATE INDEX        idx_lecture_topics_lecture ON lecture_topics(user_id, lecture_id);
CREATE INDEX        idx_lecture_topics_topic   ON lecture_topics(user_id, topic_id);
