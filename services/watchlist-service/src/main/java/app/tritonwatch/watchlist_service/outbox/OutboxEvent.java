package app.tritonwatch.watchlist_service.outbox;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
@NoArgsConstructor
@Setter
@Getter
public class OutboxEvent {

    @Id
    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "aggregate_type", nullable = false, length = 80)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 80)
    private OutboxEventType eventType;

    @Column(nullable = false, length = 200)
    private String topic;

    @Column(name = "message_key", nullable = false, length = 100)
    private String messageKey;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;

    public static OutboxEvent pending(
            UUID eventId,
            String aggregateType,
            UUID aggregateId,
            OutboxEventType eventType,
            String topic,
            String messageKey,
            String payload,
            Instant occurredAt
    ) {
        OutboxEvent event = new OutboxEvent();
        event.eventId = eventId;
        event.aggregateType = aggregateType;
        event.aggregateId = aggregateId;
        event.eventType = eventType;
        event.topic = topic;
        event.messageKey = messageKey;
        event.payload = payload;
        event.occurredAt = occurredAt;
        event.attempts = 0;
        return event;
    }

    public void markPublished(Instant publishedAt) {
        this.publishedAt = publishedAt;
        this.lastError = null;
    }

    public void markFailed(Throwable exception) {
        this.attempts++;

        String message = exception.getMessage();
        this.lastError = message == null
                ? exception.getClass().getName()
                : exception.getClass().getName() + ": " + message;
    }

}
