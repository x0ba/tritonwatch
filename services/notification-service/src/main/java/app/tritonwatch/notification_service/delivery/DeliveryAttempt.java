package app.tritonwatch.notification_service.delivery;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@NoArgsConstructor
@Entity
@Table(
        name = "delivery_attempts",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_delivery_attempts_event_user_channel",
                columnNames = {"availability_event_id", "user_id", "channel"}
        )
)
public class DeliveryAttempt {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @Column(name = "availability_event_id", nullable = false, updatable = false)
    private UUID availabilityEventId;

    @NotBlank
    @Size(max = 255)
    @Column(name = "user_id", nullable = false, updatable = false, length = 255)
    private String userId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 20)
    private DeliveryChannel channel;

    @NotBlank
    @Size(max = 50)
    @Column(name = "course_id", nullable = false, updatable = false, length = 50)
    private String courseId;

    @NotBlank
    @Size(max = 20)
    @Column(nullable = false, updatable = false, length = 20)
    private String term;

    @NotBlank
    @Size(max = 320)
    @Column(nullable = false, length = 320)
    private String destination;

    @Column(name = "open_seat_count", nullable = false, updatable = false)
    private int openSeatCount;

    @Column(name = "open_package_count", nullable = false, updatable = false)
    private int openPackageCount;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeliveryStatus status;

    @Size(max = 120)
    @Column(name = "provider_message_id", length = 120)
    private String providerMessageId;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;

    @NotNull
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @NotNull
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    public static DeliveryAttempt pending(
            UUID id,
            UUID availabilityEventId,
            String userId,
            DeliveryChannel channel,
            String courseId,
            String term,
            String destination,
            int openSeatCount,
            int openPackageCount,
            Instant now
    ) {
        DeliveryAttempt attempt = new DeliveryAttempt();
        attempt.id = id;
        attempt.availabilityEventId = availabilityEventId;
        attempt.userId = userId;
        attempt.channel = channel;
        attempt.courseId = courseId;
        attempt.term = term;
        attempt.destination = destination;
        attempt.openSeatCount = openSeatCount;
        attempt.openPackageCount = openPackageCount;
        attempt.status = DeliveryStatus.PENDING;
        attempt.attempts = 0;
        attempt.createdAt = now;
        attempt.updatedAt = now;
        return attempt;
    }

    public void markSent(String providerMessageId, Instant now) {
        this.status = DeliveryStatus.SENT;
        this.providerMessageId = providerMessageId;
        this.lastError = null;
        this.sentAt = now;
        this.updatedAt = now;
        this.attempts++;
    }

    public void markFailed(Throwable exception, Instant now, int maxAttempts) {
        this.attempts++;
        this.updatedAt = now;
        String message = exception.getMessage();
        this.lastError = message == null
                ? exception.getClass().getName()
                : exception.getClass().getName() + ": " + message;
        if (this.attempts >= maxAttempts) {
            this.status = DeliveryStatus.FAILED;
        }
    }
}
