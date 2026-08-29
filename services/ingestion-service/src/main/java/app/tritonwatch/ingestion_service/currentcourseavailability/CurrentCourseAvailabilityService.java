package app.tritonwatch.ingestion_service.currentcourseavailability;

import app.tritonwatch.ingestion_service.outbox.OutboxEventWriter;
import app.tritonwatch.ingestion_service.ucsd.dto.CatalogCourseResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentCourseAvailabilityService {

    private final CurrentCourseAvailabilityRepository currentCourseAvailabilityRepository;
    private final OutboxEventWriter outboxEventWriter;

    @Transactional
    public void process(CatalogCourseResponse course) {
        if (course.availabilityRefreshPending()) {
            return;
        }

        var existing = currentCourseAvailabilityRepository
                .findByTermAndCourseId(course.termCode(), course.moduleCode());

        if (existing.isEmpty()) {
            currentCourseAvailabilityRepository.save(CurrentCourseAvailability.from(course));
            return;
        }

        var availability = existing.get();

        boolean wasAvailable =
                availability.getOpenSeatCount() > 0
                        || availability.getOpenPackageCount() > 0;

        boolean isAvailable =
                course.openSeatCount() > 0
                        || course.openPackageCount() > 0;

        int previousOpenSeatCount = availability.getOpenSeatCount();
        int previousOpenPackageCount = availability.getOpenPackageCount();

        availability.setOpenSeatCount(course.openSeatCount());
        availability.setOpenPackageCount(course.openPackageCount());

        if (!wasAvailable && isAvailable) {
            outboxEventWriter.appendCourseSectionBecameAvailable(
                    availability,
                    previousOpenSeatCount,
                    previousOpenPackageCount
            );
        }
    }
}
