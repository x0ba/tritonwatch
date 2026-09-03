package app.tritonwatch.notification_service.subscription;

import app.tritonwatch.contracts.event.UserCourseWatchCreated;
import app.tritonwatch.contracts.event.UserCourseWatchDeleted;
import app.tritonwatch.contracts.kafka.KafkaTopics;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SubscriptionConsumer {
    private final SubscriptionService subscriptionService;

    @KafkaListener(topics = KafkaTopics.USER_COURSE_WATCH_CREATED, groupId = "notification-service")
    public void consumeUserCourseWatchCreated(UserCourseWatchCreated event) {
        subscriptionService.create(event);
    }

    @KafkaListener(topics = KafkaTopics.USER_COURSE_WATCH_DELETED, groupId = "notification-service")
    public void consumeUserCourseWatchDeleted(UserCourseWatchDeleted event) {
        subscriptionService.delete(event);
    }
}
