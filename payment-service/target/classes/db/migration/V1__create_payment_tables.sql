CREATE TABLE payment_intents (
    id                  CHAR(36)      NOT NULL,
    milestone_id        CHAR(36)      NOT NULL,
    contract_id         CHAR(36)      NOT NULL,
    buyer_id            CHAR(36)      NOT NULL,
    amount              DECIMAL(12,2) NOT NULL,
    currency            VARCHAR(10)   NOT NULL DEFAULT 'USD',
    provider            VARCHAR(30)   NOT NULL,
    provider_order_id   VARCHAR(255),
    provider_payment_id VARCHAR(255),
    status              VARCHAR(30)   NOT NULL,
    idempotency_key     VARCHAR(255)  NOT NULL,
    retry_count         INT           NOT NULL DEFAULT 0,
    version             BIGINT        NOT NULL DEFAULT 0,
    created_at          DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE INDEX idx_pi_idempotency (idempotency_key),
    INDEX idx_pi_buyer (buyer_id),
    INDEX idx_pi_milestone_status (milestone_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE webhook_events (
    id                  CHAR(36)     NOT NULL,
    provider            VARCHAR(30)  NOT NULL,
    provider_event_id   VARCHAR(255) NOT NULL,
    event_type          VARCHAR(100),
    raw_payload         LONGTEXT,
    processed           BOOLEAN      NOT NULL DEFAULT FALSE,
    processed_at        DATETIME(6),
    processing_error    VARCHAR(1000),
    created_at          DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE INDEX idx_we_provider_event (provider, provider_event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
