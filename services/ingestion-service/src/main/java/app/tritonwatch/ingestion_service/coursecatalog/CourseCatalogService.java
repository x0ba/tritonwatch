package app.tritonwatch.ingestion_service.coursecatalog;

import app.tritonwatch.ingestion_service.ucsd.CourseIds;
import app.tritonwatch.ingestion_service.ucsd.UcsdCatalogClient;
import app.tritonwatch.ingestion_service.ucsd.dto.PlannerTermsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CourseCatalogService {

    private static final int DEFAULT_LIMIT = 25;
    private static final int MAX_LIMIT = 50;

    private final CourseCatalogRepository courseCatalogRepository;
    private final UcsdCatalogClient ucsdCatalogClient;

    public List<CourseCatalogEntry> search(String term, String query, Integer limit) {
        String normalizedTerm = resolveTerm(term);
        String normalizedQuery = query == null ? "" : query.trim();
        if (normalizedQuery.isEmpty()) {
            return List.of();
        }

        int pageSize = limit == null ? DEFAULT_LIMIT : Math.clamp(limit, 1, MAX_LIMIT);
        return courseCatalogRepository.search(
                normalizedTerm,
                normalizedQuery,
                PageRequest.of(0, pageSize)
        );
    }

    public List<CourseCatalogEntry> findByIds(String term, List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        String normalizedTerm = resolveTerm(term);

        List<String> normalizedIds = ids.stream()
                .filter(Objects::nonNull)
                .map(CourseIds::normalize)
                .filter(id -> !id.isBlank())
                .distinct()
                .limit(100)
                .toList();

        if (normalizedIds.isEmpty()) {
            return List.of();
        }

        return courseCatalogRepository.findByTermAndCourseIdIn(normalizedTerm, normalizedIds);
    }

    public List<TermOption> listTerms() {
        return ucsdCatalogClient.listTerms().stream()
                .filter(PlannerTermsResponse.PlannerTerm::configured)
                .map(term -> new TermOption(
                        term.termCode(),
                        TermLabels.labelFor(term.termCode())
                ))
                .toList();
    }

    public String currentTerm() {
        return ucsdCatalogClient.getCurrentTerm();
    }

    private String resolveTerm(String term) {
        if (term == null || term.isBlank()) {
            return currentTerm();
        }
        return CourseIds.normalize(term);
    }

    public record TermOption(String code, String label) {
    }
}
