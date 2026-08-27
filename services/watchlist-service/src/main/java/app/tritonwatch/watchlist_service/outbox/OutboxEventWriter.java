package app.tritonwatch.watchlist_service.outbox;

import app.tritonwatch.contracts.event.CourseTrackingRequested;
import app.tritonwatch.contracts.event.UserCourseWatchCreated;
import app.tritonwatch.contracts.kafka.KafkaTopics;
import app.tritonwatch.watchlist_service.watchrequest.WatchRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OutboxEventWriter {

    private static final String WATCH_REQUEST_AGGREGATE = "WATCH_REQUEST";

    private final OutboxEventRepository outboxEventRepository;
    private final JsonMapper jsonMapper;

    public void appendWatchRequestEvents(WatchRequest request) {
        String key = request.getCourseId() + ":" + request.getTerm();

        UUID watchCreatedEventId = UUID.randomUUID();

        var watchCreatedEvent = new UserCourseWatchCreated(
                watchCreatedEventId,
                request.getCreatedAt(),
                request.getUserId(),
                request.getCourseId(),
                request.getTerm()
        );

        UUID trackingRequestedEventId = UUID.randomUUID();

        var trackingRequestedEvent = new CourseTrackingRequested(
                trackingRequestedEventId,
                request.getCreatedAt(),
                request.getCourseId(),
                request.getTerm()
        );

        OutboxEvent watchCreatedOutboxEvent = OutboxEvent.pending(
                watchCreatedEventId,
                WATCH_REQUEST_AGGREGATE,
                request.getId(),
                OutboxEventType.USER_COURSE_WATCH_CREATED,
                KafkaTopics.USER_COURSE_WATCH_CREATED,
                key,
                serialize(watchCreatedEvent),
                watchCreatedEvent.occurredAt()
        );

        OutboxEvent trackingRequestedOutboxEvent = OutboxEvent.pending(
                trackingRequestedEventId,
                WATCH_REQUEST_AGGREGATE,
                request.getId(),
                OutboxEventType.COURSE_TRACKING_REQUESTED,
                KafkaTopics.COURSE_TRACKING_REQUESTED,
                key,
                serialize(trackingRequestedEvent),
                trackingRequestedEvent.occurredAt()
        );

        outboxEventRepository.saveAll(List.of(
                watchCreatedOutboxEvent,
                trackingRequestedOutboxEvent
        ));
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
