package app.tritonwatch.user_service.outbox;

import app.tritonwatch.contracts.event.UserNotificationSettingsUpdated;
import app.tritonwatch.contracts.kafka.KafkaTopics;
import app.tritonwatch.user_service.notificationpreference.NotificationPreference;
import app.tritonwatch.user_service.userprofile.UserProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OutboxEventWriter {

    private static final String USER_PROFILE_AGGREGATE = "USER_PROFILE";

    private final OutboxEventRepository outboxEventRepository;
    private final JsonMapper jsonMapper;

    public void appendSettingsUpdated(
            UserProfile profile,
            NotificationPreference preference,
            Instant occurredAt
    ) {
        UUID eventId = UUID.randomUUID();
        var event = new UserNotificationSettingsUpdated(
                eventId,
                occurredAt,
                profile.getUserId(),
                profile.getProfileVersion(),
                profile.getStatus().name(),
                profile.getEmail(),
                profile.getEmailVerifiedAt() != null,
                profile.getPhoneE164(),
                profile.getPhoneVerifiedAt() != null,
                preference.isEmailEnabled(),
                preference.isSmsEnabled()
        );

        outboxEventRepository.save(OutboxEvent.pending(
                eventId,
                USER_PROFILE_AGGREGATE,
                profile.getUserId(),
                OutboxEventType.USER_NOTIFICATION_SETTINGS_UPDATED,
                KafkaTopics.USER_NOTIFICATION_SETTINGS_UPDATED,
                profile.getUserId(),
                serialize(event),
                occurredAt
        ));
    }

    private String serialize(Object event) {
        try {
            return jsonMapper.writeValueAsString(event);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Could not serialize outbox event", exception);
        }
    }
}
