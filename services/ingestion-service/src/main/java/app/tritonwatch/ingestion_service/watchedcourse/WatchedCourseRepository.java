package app.tritonwatch.ingestion_service.watchedcourse;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WatchedCourseRepository extends JpaRepository<WatchedCourse, UUID> {
}
