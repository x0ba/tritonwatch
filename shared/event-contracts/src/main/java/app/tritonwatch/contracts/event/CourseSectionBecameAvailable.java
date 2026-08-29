package app.tritonwatch.contracts.event;

import java.time.Instant;
import java.util.UUID;

public record CourseSectionBecameAvailable(
        UUID eventId,
        Instant occurredAt,
        String courseId,
        String term,
        int openSeatCount,
        int previousOpenSeatCount,
        int openPackageCount,
        int previousOpenPackageCount
) {
}
