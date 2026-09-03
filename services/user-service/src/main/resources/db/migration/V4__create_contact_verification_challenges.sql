CREATE TABLE contact_verification_challenges
(
    id           UUID PRIMARY KEY,
    user_id      VARCHAR(255) NOT NULL,
    channel      VARCHAR(20)  NOT NULL,
    destination  VARCHAR(320) NOT NULL,
    code_hash    VARCHAR(128) NOT NULL,
    expires_at   TIMESTAMPTZ  NOT NULL,
    consumed_at  TIMESTAMPTZ,
    created_at   TIMESTAMPTZ  NOT NULL,

    CONSTRAINT uq_contact_verification_challenges_user_channel
        UNIQUE (user_id, channel)
);

CREATE INDEX idx_contact_verification_challenges_expires
    ON contact_verification_challenges (expires_at);
