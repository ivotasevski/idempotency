-------------------------------------------
---------- transactional_outbox -----------
-------------------------------------------
CREATE TABLE IF NOT EXISTS gtw_idemp
(
    id                   BIGSERIAL      PRIMARY KEY,
    x_request_id         VARCHAR(255)   NOT NULL,
    trx_id               VARCHAR(255)   NOT NULL,
    status               VARCHAR(255)     NOT NULL,
    idempotent_action    VARCHAR(255)     NOT NULL,
    created_at           TIMESTAMP      NOT NULL,
    updated_at           TIMESTAMP      NOT NULL,
    expired_at           TIMESTAMP      NOT NULL,
    lock_deadline        TIMESTAMP      NOT NULL,
    request_hash         VARCHAR(255),
    response_code        INTEGER,
    response_body        BYTEA,
    response_headers     JSONB,
    version              NUMERIC        DEFAULT 0,
    CONSTRAINT unique_gtw_idemp_x_request_id UNIQUE (x_request_id)
);