package app.tritonwatch.notification_service.usersettings;

import app.tritonwatch.contracts.event.UserNotificationSettingsUpdated;
import app.tritonwatch.contracts.kafka.KafkaTopics;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserNotificationSettingsConsumer {

    private final UserNotificationSettingsService userNotificationSettingsService;

    @KafkaListener(topics = KafkaTopics.USER_NOTIFICATION_SETTINGS_UPDATED, groupId = "notification-service")
    public void consumeUserNotificationSettingsUpdated(UserNotificationSettingsUpdated event) {
        userNotificationSettingsService.upsert(event);
    }
}
