INSERT INTO learning_profile (user_id, preferred_explanation_styles, struggles, tone_preference, learning_goals)
VALUES (
    1,
    '["step_by_step","code_first","dry_run","examples","practical_usage","mistake_based_learning"]',
    '["long_abstract_theory","unstructured_topics","restarting_after_gap","forgetting_revision","too_many_pending_topics","losing_context_after_break"]',
    'direct, friendly, motivating, bro-style, technically clear',
    '["Complete DSA for interviews","Build LLD/HLD knowledge","Master SQL"]'
)
ON CONFLICT (user_id) DO NOTHING;
