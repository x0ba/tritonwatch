package app.tritonwatch.ingestion_service.coursecatalog;

import app.tritonwatch.ingestion_service.coursecatalog.dto.CourseCatalogItemResponse;
import app.tritonwatch.ingestion_service.coursecatalog.dto.CourseSearchApiResponse;
import app.tritonwatch.ingestion_service.coursecatalog.dto.TermOptionResponse;
import app.tritonwatch.ingestion_service.coursecatalog.dto.TermsApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/catalog")
@RequiredArgsConstructor
public class CourseCatalogController {

    private final CourseCatalogService courseCatalogService;
    private final CourseCatalogSyncService courseCatalogSyncService;

    @GetMapping("/courses")
    public CourseSearchApiResponse searchCourses(
            @RequestParam(required = false) String term,
            @RequestParam(required = false, defaultValue = "") String q,
            @RequestParam(required = false) Integer limit
    ) {
        String resolvedTerm = term == null || term.isBlank()
                ? courseCatalogService.currentTerm()
                : term.trim().toUpperCase();

        List<CourseCatalogItemResponse> courses = courseCatalogService.search(term, q, limit).stream()
                .map(entry -> new CourseCatalogItemResponse(
                        entry.getCourseId(),
                        entry.getTitle(),
                        entry.getOpenSeatCount(),
                        entry.getWaitlistCount()
                ))
                .toList();

        return new CourseSearchApiResponse(resolvedTerm, q.trim(), courses.size(), courses);
    }

    @GetMapping("/terms")
    public TermsApiResponse listTerms() {
        String currentTerm = courseCatalogService.currentTerm();
        List<TermOptionResponse> terms = courseCatalogService.listTerms().stream()
                .map(option -> new TermOptionResponse(option.code(), option.label()))
                .toList();
        return new TermsApiResponse(currentTerm, terms);
    }

    @PostMapping("/sync")
    public ResponseEntity<Map<String, Object>> manualOpsSync() {
        int count = courseCatalogSyncService.syncCurrentTerm();
        return ResponseEntity.ok(Map.of(
                "synced", count,
                "term", courseCatalogService.currentTerm()
        ));
    }
}
