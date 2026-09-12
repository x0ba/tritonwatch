package app.tritonwatch.ingestion_service.watchedcourse;

import app.tritonwatch.contracts.event.CourseTrackingRequested;
import app.tritonwatch.contracts.event.CourseTrackingStopped;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WatchedCourseService {

    private final WatchedCourseRepository watchedCourseRepository;

    @Transactional
    public void create(CourseTrackingRequested event) {
        watchedCourseRepository.insertIfAbsent(
                UUID.randomUUID(),
                event.courseId().trim().toUpperCase(Locale.ROOT),
                event.term().trim().toUpperCase(Locale.ROOT)
        );
    }

    @Transactional
    public void remove(CourseTrackingStopped event) {
        watchedCourseRepository.deleteByCourseIdAndTerm(
                event.courseId().trim().toUpperCase(Locale.ROOT),
                event.term().trim().toUpperCase(Locale.ROOT)
        );
    }
}
