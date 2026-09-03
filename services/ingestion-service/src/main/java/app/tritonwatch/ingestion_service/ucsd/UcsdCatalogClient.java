package app.tritonwatch.ingestion_service.ucsd;

import app.tritonwatch.ingestion_service.ucsd.dto.CourseSearchRequest;
import app.tritonwatch.ingestion_service.ucsd.dto.CourseSearchResponse;
import app.tritonwatch.ingestion_service.ucsd.dto.PlannerTermsResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class UcsdCatalogClient {

    private final RestClient restClient;

    public UcsdCatalogClient(RestClient ucsdRestClient) {
        this.restClient = ucsdRestClient;
    }

    public CourseSearchResponse search(String term, List<String> courseIds, int limit, int offset) {
        var request = new CourseSearchRequest(term, courseIds, limit, offset);

        return restClient.post()
                .uri("/api/v1/catalog/courses/search")
                .body(request)
                .retrieve()
                .body(CourseSearchResponse.class);
    }

    public CourseSearchResponse searchPage(String term, int limit, int offset) {
        return search(term, List.of(), limit, offset);
    }

    public List<PlannerTermsResponse.PlannerTerm> listTerms() {
        PlannerTermsResponse response = restClient.get()
                .uri("/api/v1/planner/terms")
                .retrieve()
                .body(PlannerTermsResponse.class);

        if (response == null || response.terms() == null || response.terms().isEmpty()) {
            throw new IllegalStateException("UCSD planner API returned an empty or invalid terms response");
        }

        return response.terms();
    }

    public String getCurrentTerm() {
        return listTerms().stream()
                .filter(PlannerTermsResponse.PlannerTerm::configured)
                .map(PlannerTermsResponse.PlannerTerm::termCode)
                .findFirst()
                .or(() -> listTerms().stream()
                        .map(PlannerTermsResponse.PlannerTerm::termCode)
                        .findFirst())
                .orElseThrow(() -> new IllegalStateException("UCSD planner API returned no term codes"));
    }
}
