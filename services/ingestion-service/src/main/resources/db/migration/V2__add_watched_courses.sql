CREATE TABLE watched_courses
(
    id         UUID PRIMARY KEY,
    course_id  VARCHAR(50) NOT NULL,
    term       VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT uq_course_id_term
        UNIQUE (course_id, term)
);