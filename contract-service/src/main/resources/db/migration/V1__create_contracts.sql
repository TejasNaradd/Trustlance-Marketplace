CREATE TABLE contracts (
    id          CHAR(36)     NOT NULL,
    buyer_id    CHAR(36)     NOT NULL,
    seller_id   CHAR(36)     NOT NULL,
    service_id  CHAR(36)     NULL,
    title       VARCHAR(200) NOT NULL,
    description TEXT         NOT NULL,
    status      VARCHAR(30)  NOT NULL,
    version     BIGINT       NOT NULL DEFAULT 0,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_contract_buyer  (buyer_id),
    INDEX idx_contract_seller (seller_id),
    INDEX idx_contract_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
