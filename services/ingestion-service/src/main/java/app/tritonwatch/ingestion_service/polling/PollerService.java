package app.tritonwatch.ingestion_service.polling;

import app.tritonwatch.ingestion_service.currentcourseavailability.CurrentCourseAvailabilityService;
import app.tritonwatch.ingestion_service.ucsd.UcsdCatalogClient;
import app.tritonwatch.ingestion_service.ucsd.dto.CatalogCourseResponse;
import app.tritonwatch.ingestion_service.watchedcourse.WatchedCourse;
import app.tritonwatch.ingestion_service.watchedcourse.WatchedCourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PollerService {

    private final UcsdCatalogClient ucsdCatalogClient;
    private final WatchedCourseRepository watchedCourseRepository;
    private final CurrentCourseAvailabilityService currentCourseAvailabilityService;

    public void getWatchedCoursesForCurrentTerm() {
        String term = ucsdCatalogClient.getCurrentTerm();
        List<String> watchedCourseIds = watchedCourseRepository.findAllByTerm(term).stream()
                .map(WatchedCourse::getCourseId)
                .toList();

        if (watchedCourseIds.isEmpty()) {
            return;
        }

        int limit = 48;
        int offset = 0;

        List<CatalogCourseResponse> allCourses = new ArrayList<>();

        while (true) {
            var response = ucsdCatalogClient.search(term, watchedCourseIds, limit, offset);

            if (response == null || response.courses() == null) {
                throw new IllegalStateException("UCSD catalog API returned an invalid search response");
            }

            if (response.courses().isEmpty()) {
                break;
            }

            allCourses.addAll(response.courses());
            offset += response.courses().size();

            if (offset >= response.total()) {
                break;
            }
        }

        for (var course : allCourses) {
            currentCourseAvailabilityService.process(course);
        }

    }
}
