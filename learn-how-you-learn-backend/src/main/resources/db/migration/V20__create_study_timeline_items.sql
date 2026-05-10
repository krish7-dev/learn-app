CREATE TABLE study_timeline_items (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL DEFAULT 1,
    target_id           BIGINT NOT NULL REFERENCES learning_targets(id),
    course_id           BIGINT,
    lecture_id          BIGINT,
    topic_id            BIGINT,
    scheduled_date      DATE NOT NULL,
    item_type           VARCHAR(20) NOT NULL,
    title               VARCHAR(255) NOT NULL,
    description         TEXT,
    estimated_minutes   INT NOT NULL DEFAULT 30,
    plan_tier           VARCHAR(10) NOT NULL DEFAULT 'FULL',
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    ai_reasoning        TEXT,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_item_type CHECK (item_type IN (
        'ADD_TRANSCRIPT','GENERATE_NOTES','STUDY_LECTURE',
        'REVISION','WEAK_AREA','PRACTICE','TEACH_BACK','BUFFER'
    )),
    CONSTRAINT chk_plan_tier   CHECK (plan_tier IN ('FULL','MEDIUM','MINIMUM')),
    CONSTRAINT chk_item_status CHECK (status IN ('PENDING','DONE','SKIPPED','RESCHEDULED'))
);

CREATE INDEX idx_timeline_target_date ON study_timeline_items(target_id, scheduled_date);
CREATE INDEX idx_timeline_user_date   ON study_timeline_items(user_id, scheduled_date, status);
