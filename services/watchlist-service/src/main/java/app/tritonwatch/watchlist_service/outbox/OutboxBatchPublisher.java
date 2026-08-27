package app.tritonwatch.watchlist_service.outbox;

import app.tritonwatch.contracts.event.CourseTrackingRequested;
import app.tritonwatch.contracts.event.UserCourseWatchCreated;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxBatchPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final JsonMapper jsonMapper;

    @Value("${outbox.batch-size:50}")
    private int batchSize;

    @Value("${outbox.send-timeout-seconds:10}")
    private long sendTimeoutSeconds;

    @Transactional
    public void publishBatch() {
        for (OutboxEvent outboxEvent : outboxEventRepository.lockPendingBatch(batchSize)) {
            try {
                Object payload = deserialize(outboxEvent);

                kafkaTemplate.send(
                        outboxEvent.getTopic(),
                        outboxEvent.getMessageKey(),
                        payload
                ).get(sendTimeoutSeconds, TimeUnit.SECONDS);

                outboxEvent.markPublished(Instant.now());
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                outboxEvent.markFailed(exception);

                log.warn(
                        "Outbox relay was interrupted while publishing event {}",
                        outboxEvent.getEventId(),
                        exception
                );

                break;
            } catch (Exception exception) {
                outboxEvent.markFailed(exception);

                log.warn(
                        "Could not publish outbox event {}",
                        outboxEvent.getEventId(),
                        exception
                );
            }

        }
    }

    private Object deserialize(OutboxEvent event) throws JacksonException {

        return switch (event.getEventType()) {
            case USER_COURSE_WATCH_CREATED -> jsonMapper.readValue(
                    event.getPayload(),
                    UserCourseWatchCreated.class
            );
            case COURSE_TRACKING_REQUESTED -> jsonMapper.readValue(
                    event.getPayload(),
                    CourseTrackingRequested.class
            );
        };
    }
}
