CREATE TABLE user_notification_settings
(
    user_id           VARCHAR(255) PRIMARY KEY,
    profile_version   BIGINT       NOT NULL,
    status            VARCHAR(20)  NOT NULL,
    email             VARCHAR(320),
    email_verified    BOOLEAN      NOT NULL,
    phone_e164        VARCHAR(16),
    phone_verified    BOOLEAN      NOT NULL,
    email_enabled     BOOLEAN      NOT NULL,
    sms_enabled       BOOLEAN      NOT NULL,
    updated_at        TIMESTAMPTZ  NOT NULL
);
