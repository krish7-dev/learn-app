CREATE TABLE tree_nodes (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT NOT NULL DEFAULT 1,
    parent_id        BIGINT REFERENCES tree_nodes(id) ON DELETE CASCADE,
    label            VARCHAR(255) NOT NULL,
    normalized_label VARCHAR(255) NOT NULL,
    node_type        VARCHAR(20)  NOT NULL DEFAULT 'INTERMEDIATE',
    topic_id         BIGINT REFERENCES topics(id) ON DELETE SET NULL,
    lecture_id       BIGINT REFERENCES lectures(id) ON DELETE SET NULL,
    position         INT NOT NULL DEFAULT 0,
    created_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_tree_node_type CHECK (node_type IN ('ROOT','INTERMEDIATE','CONCEPT','SHELL'))
);

-- Sibling deduplication using normalized_label (parent_id IS NOT NULL)
CREATE UNIQUE INDEX idx_tree_nodes_sibling
    ON tree_nodes(user_id, parent_id, normalized_label)
    WHERE parent_id IS NOT NULL;

-- Root deduplication using normalized_label (parent_id IS NULL)
CREATE UNIQUE INDEX idx_tree_nodes_root
    ON tree_nodes(user_id, normalized_label)
    WHERE parent_id IS NULL;

CREATE INDEX idx_tree_nodes_parent  ON tree_nodes(parent_id);
CREATE INDEX idx_tree_nodes_topic   ON tree_nodes(topic_id);
CREATE INDEX idx_tree_nodes_lecture ON tree_nodes(lecture_id);
