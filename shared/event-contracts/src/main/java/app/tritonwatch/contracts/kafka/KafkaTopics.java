package app.tritonwatch.contracts.kafka;

/** Versioned Kafka topic names shared by producers and consumers. */
public final class KafkaTopics {
    public static final String USER_COURSE_WATCH_CREATED =
            "tritonwatch.user-course-watch-created.v1";
    public static final String COURSE_TRACKING_REQUESTED =
            "tritonwatch.course-tracking-requested.v1";
    public static final String COURSE_SECTION_BECAME_AVAILABLE =
            "tritonwatch.course-section-became-available.v1";

    private KafkaTopics() {
    }
}
