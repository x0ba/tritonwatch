package app.tritonwatch.contracts.event;

import java.time.Instant;
import java.util.UUID;

public record UserCourseWatchCreated(
        UUID eventId,
        Instant occurredAt,
        UUID userId,
        String courseId,
        String term
) {
}
