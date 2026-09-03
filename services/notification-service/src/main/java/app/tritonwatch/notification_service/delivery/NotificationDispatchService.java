package app.tritonwatch.notification_service.delivery;

import app.tritonwatch.contracts.event.CourseSectionBecameAvailable;
import app.tritonwatch.notification_service.subscription.Subscription;
import app.tritonwatch.notification_service.subscription.SubscriptionRepository;
import app.tritonwatch.notification_service.usersettings.UserNotificationSettings;
import app.tritonwatch.notification_service.usersettings.UserNotificationSettingsRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationDispatchService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserNotificationSettingsRepository userNotificationSettingsRepository;
    private final DeliveryAttemptRepository deliveryAttemptRepository;
    private final Clock clock;

    @Transactional
    public void enqueue(CourseSectionBecameAvailable event) {
        String courseId = event.courseId().trim().toUpperCase(Locale.ROOT);
        String term = event.term().trim().toUpperCase(Locale.ROOT);
        List<Subscription> subscriptions = subscriptionRepository.findByCourseIdAndTerm(courseId, term);
        if (subscriptions.isEmpty()) {
            return;
        }

        List<String> userIds = subscriptions.stream().map(Subscription::getUserId).distinct().toList();
        Map<String, UserNotificationSettings> settingsByUserId = userNotificationSettingsRepository
                .findByUserIdIn(userIds)
                .stream()
                .collect(Collectors.toMap(UserNotificationSettings::getUserId, Function.identity()));

        Instant now = clock.instant();
        for (Subscription subscription : subscriptions) {
            UserNotificationSettings settings = settingsByUserId.get(subscription.getUserId());
            if (settings == null) {
                continue;
            }

            if (settings.canReceiveEmail()) {
                deliveryAttemptRepository.insertIfAbsent(
                        UUID.randomUUID(),
                        event.eventId(),
                        subscription.getUserId(),
                        DeliveryChannel.EMAIL.name(),
                        courseId,
                        term,
                        settings.getEmail(),
                        event.openSeatCount(),
                        event.openPackageCount(),
                        now
                );
            }

            if (settings.canReceiveSms()) {
                deliveryAttemptRepository.insertIfAbsent(
                        UUID.randomUUID(),
                        event.eventId(),
                        subscription.getUserId(),
                        DeliveryChannel.SMS.name(),
                        courseId,
                        term,
                        settings.getPhoneE164(),
                        event.openSeatCount(),
                        event.openPackageCount(),
                        now
                );
            }
        }
    }
}
