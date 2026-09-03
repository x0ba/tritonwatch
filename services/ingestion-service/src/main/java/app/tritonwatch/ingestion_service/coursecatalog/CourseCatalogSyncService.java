package app.tritonwatch.ingestion_service.coursecatalog;

import app.tritonwatch.ingestion_service.ucsd.CourseIds;
import app.tritonwatch.ingestion_service.ucsd.UcsdCatalogClient;
import app.tritonwatch.ingestion_service.ucsd.dto.CatalogCourseResponse;
import app.tritonwatch.ingestion_service.ucsd.dto.CourseSearchResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CourseCatalogSyncService {

    private static final Logger log = LoggerFactory.getLogger(CourseCatalogSyncService.class);
    private static final int PAGE_SIZE = 48;

    private final UcsdCatalogClient ucsdCatalogClient;
    private final CourseCatalogRepository courseCatalogRepository;
    private final TransactionTemplate transactionTemplate;

    public int syncCurrentTerm() {
        return syncTerm(ucsdCatalogClient.getCurrentTerm());
    }

    public int syncTerm(String term) {
        Instant refreshedAt = Instant.now();
        Map<String, CatalogCourseResponse> coursesById = fetchAllCourses(term);

        List<CourseCatalogEntry> entries = new ArrayList<>(coursesById.size());
        for (Map.Entry<String, CatalogCourseResponse> item : coursesById.entrySet()) {
            entries.add(toEntry(term, item.getKey(), item.getValue(), refreshedAt));
        }

        transactionTemplate.executeWithoutResult(status -> {
            courseCatalogRepository.deleteByTerm(term);
            courseCatalogRepository.saveAll(entries);
        });

        log.info("Synced course catalog for term {}: {} courses", term, entries.size());
        return entries.size();
    }

    private Map<String, CatalogCourseResponse> fetchAllCourses(String term) {
        Map<String, CatalogCourseResponse> coursesById = new LinkedHashMap<>();
        int offset = 0;

        while (true) {
            CourseSearchResponse response = ucsdCatalogClient.searchPage(term, PAGE_SIZE, offset);
            if (response == null || response.courses() == null) {
                throw new IllegalStateException("UCSD catalog API returned an invalid search response");
            }

            List<CatalogCourseResponse> page = response.courses();
            if (page.isEmpty()) {
                break;
            }

            for (CatalogCourseResponse course : page) {
                coursesById.put(CourseIds.fromCatalogCourse(course), course);
            }

            offset += page.size();
            if (offset >= response.total()) {
                break;
            }
        }

        return coursesById;
    }

    private static CourseCatalogEntry toEntry(
            String term,
            String courseId,
            CatalogCourseResponse course,
            Instant refreshedAt
    ) {
        CourseCatalogEntry entry = new CourseCatalogEntry();
        entry.setId(UUID.randomUUID());
        entry.setTerm(term);
        entry.setCourseId(courseId);
        entry.setTitle(
                course.moduleName() == null || course.moduleName().isBlank()
                        ? courseId
                        : course.moduleName().trim()
        );
        entry.setOpenSeatCount(course.openSeatCount());
        entry.setWaitlistCount(course.waitlistPackageCount());
        entry.setRefreshedAt(refreshedAt);
        return entry;
    }
}
