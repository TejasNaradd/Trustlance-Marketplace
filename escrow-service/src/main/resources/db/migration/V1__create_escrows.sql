CREATE TABLE escrows (
    id          CHAR(36)     NOT NULL,
    milestone_id CHAR(36)    NOT NULL,
    contract_id CHAR(36)     NOT NULL,
    buyer_id    CHAR(36)     NOT NULL,
    seller_id   CHAR(36)     NOT NULL,
    amount      DECIMAL(12,2) NOT NULL,
    currency    VARCHAR(10)  NOT NULL DEFAULT 'USD',
    status      VARCHAR(30)  NOT NULL,
    version     BIGINT       NOT NULL DEFAULT 0,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE INDEX idx_escrow_milestone (milestone_id),
    INDEX idx_escrow_contract (contract_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE escrow_transactions (
    id              CHAR(36)     NOT NULL,
    escrow_id       CHAR(36)     NOT NULL,
    milestone_id    CHAR(36)     NOT NULL,
    from_status     VARCHAR(30),
    to_status       VARCHAR(30)  NOT NULL,
    amount          DECIMAL(12,2) NOT NULL,
    currency        VARCHAR(10)  NOT NULL,
    actor_id        CHAR(36),
    actor_role      VARCHAR(30),
    reason          VARCHAR(500),
    idempotency_key VARCHAR(255),
    created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_etx_escrow (escrow_id),
    UNIQUE INDEX idx_etx_idempotency (idempotency_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
