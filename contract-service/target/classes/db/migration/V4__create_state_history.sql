CREATE TABLE state_history (
    id          CHAR(36)    NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id   CHAR(36)    NOT NULL,
    from_status VARCHAR(30) NULL,
    to_status   VARCHAR(30) NOT NULL,
    actor_id    CHAR(36)    NULL,
    actor_role  VARCHAR(20) NULL,
    reason      TEXT        NULL,
    occurred_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_state_history_entity (entity_type, entity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
