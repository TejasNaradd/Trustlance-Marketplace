CREATE TABLE submissions (
    id                  CHAR(36)    NOT NULL,
    milestone_id        CHAR(36)    NOT NULL,
    seller_id           CHAR(36)    NOT NULL,
    notes               TEXT        NULL,
    object_storage_refs TEXT        NULL,
    submitted_at        DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_submission_milestone FOREIGN KEY (milestone_id) REFERENCES milestones(id),
    INDEX idx_submission_milestone (milestone_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
