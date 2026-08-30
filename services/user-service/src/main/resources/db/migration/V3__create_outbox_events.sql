CREATE TABLE outbox_events
(
    event_id       UUID PRIMARY KEY,
    aggregate_type VARCHAR(80)  NOT NULL,
    aggregate_id   VARCHAR(255) NOT NULL,
    event_type     VARCHAR(80)  NOT NULL,
    topic          VARCHAR(200) NOT NULL,
    message_key    VARCHAR(255) NOT NULL,
    payload        TEXT         NOT NULL,
    occurred_at    TIMESTAMPTZ  NOT NULL,
    published_at   TIMESTAMPTZ,
    attempts       INTEGER      NOT NULL DEFAULT 0,
    last_error     TEXT,

    CONSTRAINT ck_outbox_events_attempts
        CHECK (attempts >= 0)
);

CREATE INDEX idx_outbox_events_pending
    ON outbox_events (occurred_at, event_id)
    WHERE published_at IS NULL;
