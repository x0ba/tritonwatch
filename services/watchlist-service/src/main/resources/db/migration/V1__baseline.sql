CREATE TABLE watch_requests
(
    id         UUID PRIMARY KEY,
    user_id    UUID        NOT NULL,
    course_id  VARCHAR(50) NOT NULL,
    term       VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT uq_watch_requests_user_course_term
        UNIQUE (user_id, course_id, term)
)
