package app.tritonwatch.contracts.event;

import java.time.Instant;
import java.util.UUID;

public record CourseSectionChanged(
        UUID eventId,
        Instant occurredAt,
        String courseId,
        String sectionId,
        String term,
        int capacity,
        int availableSeats,
        int previousAvailableSeats
) {
}
