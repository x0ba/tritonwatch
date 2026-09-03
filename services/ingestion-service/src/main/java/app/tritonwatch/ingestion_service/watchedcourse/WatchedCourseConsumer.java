package app.tritonwatch.ingestion_service.watchedcourse;

import app.tritonwatch.contracts.event.CourseTrackingRequested;
import app.tritonwatch.contracts.event.CourseTrackingStopped;
import app.tritonwatch.contracts.kafka.KafkaTopics;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WatchedCourseConsumer {

    private final WatchedCourseService watchedCourseService;

    @KafkaListener(topics = KafkaTopics.COURSE_TRACKING_REQUESTED, groupId = "ingestion-service")
    public void consumeCourseTrackingRequested(CourseTrackingRequested event) {
        watchedCourseService.create(event);
    }

    @KafkaListener(topics = KafkaTopics.COURSE_TRACKING_STOPPED, groupId = "ingestion-service")
    public void consumeCourseTrackingStopped(CourseTrackingStopped event) {
        watchedCourseService.remove(event);
    }
}
