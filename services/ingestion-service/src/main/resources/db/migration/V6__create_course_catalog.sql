CREATE TABLE course_catalog_entries
(
    id               UUID PRIMARY KEY,
    term             VARCHAR(20)  NOT NULL,
    course_id        VARCHAR(50)  NOT NULL,
    title            VARCHAR(255) NOT NULL,
    open_seat_count  INT          NOT NULL,
    waitlist_count   INT          NOT NULL,
    refreshed_at     TIMESTAMPTZ  NOT NULL,

    CONSTRAINT uq_course_catalog_term_course
        UNIQUE (term, course_id)
);

CREATE INDEX idx_course_catalog_term_course_id
    ON course_catalog_entries (term, course_id);

CREATE INDEX idx_course_catalog_term_title
    ON course_catalog_entries (term, title);
