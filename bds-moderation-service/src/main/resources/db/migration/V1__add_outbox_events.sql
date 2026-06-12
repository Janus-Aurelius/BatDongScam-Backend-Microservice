CREATE TABLE outbox_events
(
    id           UUID         NOT NULL DEFAULT gen_random_uuid(),
    topic        VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    payload      TEXT         NOT NULL,
    processed    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP    NOT NULL DEFAULT now(),
    processed_at TIMESTAMP,
    retry_count  INTEGER      NOT NULL DEFAULT 0,
    last_error   TEXT,

    CONSTRAINT pk_mod_outbox_events PRIMARY KEY (id)
);

CREATE INDEX idx_mod_outbox_processed_created ON outbox_events (processed, created_at)
    WHERE processed = FALSE;