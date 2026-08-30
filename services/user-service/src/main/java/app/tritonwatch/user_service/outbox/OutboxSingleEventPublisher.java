package app.tritonwatch.user_service.outbox;

import app.tritonwatch.contracts.event.UserNotificationSettingsUpdated;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxSingleEventPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final JsonMapper jsonMapper;

    @Value("${outbox.send-timeout-seconds:10}")
    private long sendTimeoutSeconds;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean publish(UUID eventId) {
        return outboxEventRepository.lockPendingById(eventId)
                .map(this::publishLocked)
                .orElse(true);
    }

    private boolean publishLocked(OutboxEvent outboxEvent) {
        try {
            Object payload = deserialize(outboxEvent);
            kafkaTemplate.send(
                    outboxEvent.getTopic(),
                    outboxEvent.getMessageKey(),
                    payload
            ).get(sendTimeoutSeconds, TimeUnit.SECONDS);

            outboxEvent.markPublished(Instant.now());
            return true;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            outboxEvent.markFailed(exception);
            log.warn("Outbox relay was interrupted while publishing event {}", outboxEvent.getEventId(), exception);
            return false;
        } catch (Exception exception) {
            outboxEvent.markFailed(exception);
            log.warn("Could not publish outbox event {}", outboxEvent.getEventId(), exception);
            return true;
        }
    }

    private Object deserialize(OutboxEvent event) throws JacksonException {
        return switch (event.getEventType()) {
            case USER_NOTIFICATION_SETTINGS_UPDATED -> jsonMapper.readValue(
                    event.getPayload(),
                    UserNotificationSettingsUpdated.class
            );
        };
    }
}
