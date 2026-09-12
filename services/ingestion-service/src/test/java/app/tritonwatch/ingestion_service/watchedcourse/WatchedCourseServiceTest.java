package app.tritonwatch.ingestion_service.watchedcourse;

import app.tritonwatch.contracts.event.CourseTrackingStopped;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WatchedCourseServiceTest {

    @Mock
    private WatchedCourseRepository watchedCourseRepository;

    @InjectMocks
    private WatchedCourseService watchedCourseService;

    @Test
    void removeDeletesTheCourseFromTheIngestionWatchList() {
        CourseTrackingStopped event = new CourseTrackingStopped(
                UUID.randomUUID(),
                Instant.parse("2026-09-03T12:00:00Z"),
                "cse 100",
                "fa26"
        );

        watchedCourseService.remove(event);

        verify(watchedCourseRepository).deleteByCourseIdAndTerm("CSE 100", "FA26");
    }
}
