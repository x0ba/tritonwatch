CREATE TABLE current_course_availability
(
    id                 UUID PRIMARY KEY,
    term               VARCHAR(20) NOT NULL,
    course_id          VARCHAR(50) NOT NULL,
    open_seat_count    INT         NOT NULL,
    open_package_count INT         NOT NULL,

    CONSTRAINT uq_term_course
        UNIQUE (term, course_id)
);