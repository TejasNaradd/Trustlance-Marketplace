CREATE TABLE milestones (
    id            CHAR(36)       NOT NULL,
    contract_id   CHAR(36)       NOT NULL,
    title         VARCHAR(200)   NOT NULL,
    description   TEXT           NULL,
    amount        DECIMAL(12, 2) NOT NULL,
    currency      VARCHAR(10)    NOT NULL DEFAULT 'USD',
    delivery_days INT            NOT NULL,
    status        VARCHAR(30)    NOT NULL,
    version       BIGINT         NOT NULL DEFAULT 0,
    created_at    DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at    DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_milestone_contract FOREIGN KEY (contract_id) REFERENCES contracts(id),
    INDEX idx_milestone_contract (contract_id),
    INDEX idx_milestone_status   (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
