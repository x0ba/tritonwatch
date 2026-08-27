package app.tritonwatch.watchlist_service.watchrequest;

import app.tritonwatch.contracts.event.CourseTrackingRequested;
import app.tritonwatch.contracts.event.UserCourseWatchCreated;
import app.tritonwatch.contracts.kafka.KafkaTopics;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class WatchRequestProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishUserCourseWatchCreated(WatchRequest watchRequest) {
        var event = new UserCourseWatchCreated(
                UUID.randomUUID(),
                watchRequest.getCreatedAt(),
                watchRequest.getUserId(),
                watchRequest.getCourseId(),
                watchRequest.getTerm()
        );

        String key = watchRequest.getCourseId() + ":" + watchRequest.getTerm();

        kafkaTemplate.send(
                KafkaTopics.USER_COURSE_WATCH_CREATED,
                key,
                event
        ).join();
    }

    public void publishCourseTrackingRequested(WatchRequest watchRequest) {
        var event = new CourseTrackingRequested(
                UUID.randomUUID(),
                watchRequest.getCreatedAt(),
                watchRequest.getCourseId(),
                watchRequest.getTerm()
        );

        String key = watchRequest.getCourseId() + ":" + watchRequest.getTerm();

        kafkaTemplate.send(
                KafkaTopics.COURSE_TRACKING_REQUESTED,
                key,
                event
        ).join();
    }
}
