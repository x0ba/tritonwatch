package app.tritonwatch.ingestion_service.outbox;

import app.tritonwatch.contracts.event.CourseSectionBecameAvailable;
import app.tritonwatch.contracts.kafka.KafkaTopics;
import app.tritonwatch.ingestion_service.currentcourseavailability.CurrentCourseAvailability;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OutboxEventWriter {

    private static final String CURRENT_COURSE_AVAILABILITY_AGGREGATE =
            "CURRENT_COURSE_AVAILABILITY";

    private final OutboxEventRepository outboxEventRepository;
    private final JsonMapper jsonMapper;

    public void appendCourseSectionBecameAvailable(
            CurrentCourseAvailability availability,
            int previousOpenSeatCount,
            int previousOpenPackageCount
    ) {
        UUID eventId = UUID.randomUUID();
        Instant occurredAt = Instant.now();

        var event = new CourseSectionBecameAvailable(
                eventId,
                occurredAt,
                availability.getCourseId(),
                availability.getTerm(),
                availability.getOpenSeatCount(),
                previousOpenSeatCount,
                availability.getOpenPackageCount(),
                previousOpenPackageCount
        );

        OutboxEvent outboxEvent = OutboxEvent.pending(
                eventId,
                CURRENT_COURSE_AVAILABILITY_AGGREGATE,
                availability.getId(),
                OutboxEventType.COURSE_SECTION_BECAME_AVAILABLE,
                KafkaTopics.COURSE_SECTION_BECAME_AVAILABLE,
                availability.getCourseId() + ":" + availability.getTerm(),
                serialize(event),
                occurredAt
        );

        outboxEventRepository.save(outboxEvent);
    }

    private String serialize(Object event) {
        try {
            return jsonMapper.writeValueAsString(event);
        } catch (JacksonException exception) {
            throw new IllegalStateException(
                    "Could not serialize outbox event",
                    exception
            );
        }
    }
}
