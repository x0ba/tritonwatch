CREATE TABLE delivery_attempts
(
    id                     UUID PRIMARY KEY,
    availability_event_id  UUID         NOT NULL,
    user_id                VARCHAR(255) NOT NULL,
    channel                VARCHAR(20)  NOT NULL,
    course_id              VARCHAR(50)  NOT NULL,
    term                   VARCHAR(20)  NOT NULL,
    destination            VARCHAR(320) NOT NULL,
    open_seat_count        INTEGER      NOT NULL,
    open_package_count     INTEGER      NOT NULL,
    status                 VARCHAR(20)  NOT NULL,
    provider_message_id    VARCHAR(120),
    attempts               INTEGER      NOT NULL DEFAULT 0,
    last_error             TEXT,
    created_at             TIMESTAMPTZ  NOT NULL,
    updated_at             TIMESTAMPTZ  NOT NULL,
    sent_at                TIMESTAMPTZ,

    CONSTRAINT uq_delivery_attempts_event_user_channel
        UNIQUE (availability_event_id, user_id, channel)
);

CREATE INDEX idx_delivery_attempts_pending
    ON delivery_attempts (created_at, id)
    WHERE status = 'PENDING';
