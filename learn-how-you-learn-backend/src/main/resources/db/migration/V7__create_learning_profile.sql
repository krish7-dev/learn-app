CREATE TABLE learning_profile (
    id                           BIGSERIAL PRIMARY KEY,
    user_id                      BIGINT NOT NULL UNIQUE DEFAULT 1,
    preferred_explanation_styles JSONB,
    struggles                    JSONB,
    tone_preference              TEXT,
    learning_goals               JSONB,
    created_at                   TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at                   TIMESTAMP NOT NULL DEFAULT NOW()
);
