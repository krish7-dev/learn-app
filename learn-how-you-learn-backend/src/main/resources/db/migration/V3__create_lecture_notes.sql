CREATE TABLE lecture_notes (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL DEFAULT 1,
    course_id           BIGINT NOT NULL,
    lecture_id          BIGINT NOT NULL REFERENCES lectures(id),
    title               VARCHAR(255),
    full_clean_notes    TEXT,
    simple_explanation  TEXT,
    practical_usage     TEXT,
    examples            JSONB,
    mistakes_to_avoid   JSONB,
    edge_cases          JSONB,
    revision_notes      TEXT,
    interview_questions JSONB,
    flashcards          JSONB,
    practice_questions  JSONB,
    weak_area_checks    JSONB,
    raw_ai_response     JSONB,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_lecture_notes_user_lecture UNIQUE (user_id, lecture_id)
);

CREATE INDEX idx_lecture_notes_lecture ON lecture_notes(lecture_id);
