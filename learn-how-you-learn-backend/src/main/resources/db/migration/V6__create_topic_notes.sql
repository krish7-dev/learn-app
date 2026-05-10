CREATE TABLE topic_notes (
    id                 BIGSERIAL PRIMARY KEY,
    user_id            BIGINT NOT NULL DEFAULT 1,
    topic_id           BIGINT NOT NULL REFERENCES topics(id),
    combined_notes     TEXT,
    examples           JSONB,
    edge_cases         JSONB,
    mistakes_to_avoid  JSONB,
    patterns           JSONB,
    revision_notes     TEXT,
    source_lecture_ids JSONB,
    created_at         TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_topic_notes_topic ON topic_notes(topic_id);
