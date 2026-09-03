package app.tritonwatch.watchlist_service.outbox;

public enum OutboxEventType {
    USER_COURSE_WATCH_CREATED,
    USER_COURSE_WATCH_DELETED,
    COURSE_TRACKING_REQUESTED,
    COURSE_TRACKING_STOPPED
}
