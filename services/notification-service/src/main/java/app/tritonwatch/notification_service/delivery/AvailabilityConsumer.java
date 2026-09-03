package app.tritonwatch.notification_service.delivery;

import app.tritonwatch.contracts.event.CourseSectionBecameAvailable;
import app.tritonwatch.contracts.kafka.KafkaTopics;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AvailabilityConsumer {

    private final NotificationDispatchService notificationDispatchService;

    @KafkaListener(topics = KafkaTopics.COURSE_SECTION_BECAME_AVAILABLE, groupId = "notification-service")
    public void consumeCourseSectionBecameAvailable(CourseSectionBecameAvailable event) {
        notificationDispatchService.enqueue(event);
    }
}
