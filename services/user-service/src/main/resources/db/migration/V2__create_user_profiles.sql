CREATE TABLE user_profiles
(
    user_id           VARCHAR(255) PRIMARY KEY,
    display_name      VARCHAR(120),
    email             VARCHAR(320),
    email_verified_at TIMESTAMPTZ,
    phone_e164        VARCHAR(16),
    phone_verified_at TIMESTAMPTZ,
    status            VARCHAR(20)  NOT NULL,
    profile_version   BIGINT       NOT NULL,
    created_at        TIMESTAMPTZ  NOT NULL,
    updated_at        TIMESTAMPTZ  NOT NULL,

    CONSTRAINT ck_user_profiles_status
        CHECK (status IN ('ACTIVE', 'DISABLED', 'DELETED')),
    CONSTRAINT ck_user_profiles_version
        CHECK (profile_version >= 1),
    CONSTRAINT ck_user_profiles_phone_e164
        CHECK (phone_e164 IS NULL OR phone_e164 ~ '^\+[1-9][0-9]{7,14}$'),
    CONSTRAINT ck_user_profiles_email_verification
        CHECK (email_verified_at IS NULL OR email IS NOT NULL),
    CONSTRAINT ck_user_profiles_phone_verification
        CHECK (phone_verified_at IS NULL OR phone_e164 IS NOT NULL)
);

CREATE TABLE notification_preferences
(
    user_id       VARCHAR(255) PRIMARY KEY
        REFERENCES user_profiles (user_id) ON DELETE CASCADE,
    email_enabled BOOLEAN     NOT NULL DEFAULT FALSE,
    sms_enabled   BOOLEAN     NOT NULL DEFAULT FALSE,
    updated_at    TIMESTAMPTZ NOT NULL
);

CREATE TABLE sms_consents
(
    id             UUID PRIMARY KEY,
    user_id        VARCHAR(255) NOT NULL
        REFERENCES user_profiles (user_id) ON DELETE CASCADE,
    phone_e164     VARCHAR(16)  NOT NULL,
    action         VARCHAR(10)  NOT NULL,
    source         VARCHAR(40)  NOT NULL,
    policy_version VARCHAR(40)  NOT NULL,
    occurred_at    TIMESTAMPTZ  NOT NULL,

    CONSTRAINT ck_sms_consents_action
        CHECK (action IN ('OPT_IN', 'OPT_OUT')),
    CONSTRAINT ck_sms_consents_phone_e164
        CHECK (phone_e164 ~ '^\+[1-9][0-9]{7,14}$')
);

CREATE INDEX idx_sms_consents_user_occurred
    ON sms_consents (user_id, occurred_at DESC);
