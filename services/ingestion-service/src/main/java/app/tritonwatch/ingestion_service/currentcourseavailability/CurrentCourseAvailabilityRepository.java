package app.tritonwatch.ingestion_service.currentcourseavailability;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CurrentCourseAvailabilityRepository extends JpaRepository<CurrentCourseAvailability, UUID> {
    Optional<CurrentCourseAvailability> findByTermAndCourseId(String term, String courseId);
}
