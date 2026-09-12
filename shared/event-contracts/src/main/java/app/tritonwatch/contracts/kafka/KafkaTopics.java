package app.tritonwatch.contracts.kafka;

public final class KafkaTopics {
    public static final String USER_COURSE_WATCH_CREATED =
            "tritonwatch.user-course-watch-created.v1";
    public static final String USER_COURSE_WATCH_DELETED =
            "tritonwatch.user-course-watch-deleted.v1";
    public static final String COURSE_TRACKING_REQUESTED =
            "tritonwatch.course-tracking-requested.v1";
    public static final String COURSE_TRACKING_STOPPED =
            "tritonwatch.course-tracking-stopped.v1";
    public static final String COURSE_SECTION_BECAME_AVAILABLE =
            "tritonwatch.course-section-became-available.v1";
    public static final String USER_NOTIFICATION_SETTINGS_UPDATED =
            "tritonwatch.user-notification-settings-updated.v1";

    private KafkaTopics() {
    }
}
