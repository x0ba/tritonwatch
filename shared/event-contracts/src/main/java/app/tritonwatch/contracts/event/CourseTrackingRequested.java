package app.tritonwatch.contracts.event;

import java.time.Instant;
import java.util.UUID;

public record CourseTrackingRequested(
        UUID eventId,
        Instant occurredAt,
        String courseId,
        String term
) {
}
