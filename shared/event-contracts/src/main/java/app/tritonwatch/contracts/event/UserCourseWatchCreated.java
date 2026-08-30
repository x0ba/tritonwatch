package app.tritonwatch.contracts.event;

import java.time.Instant;
import java.util.UUID;

public record UserCourseWatchCreated(
        UUID eventId,
        Instant occurredAt,
        String userId,
        String courseId,
        String term
) {
}
