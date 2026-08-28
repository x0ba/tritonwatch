package app.tritonwatch.ingestion_service.ucsd;

import app.tritonwatch.ingestion_service.ucsd.dto.CatalogTermsResponse;
import app.tritonwatch.ingestion_service.ucsd.dto.CourseSearchRequest;
import app.tritonwatch.ingestion_service.ucsd.dto.CourseSearchResponse;
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

    public String getCurrentTerm() {
        CatalogTermsResponse response = restClient.post()
                .uri("/api/v1/catalog/terms")
                .retrieve()
                .body(CatalogTermsResponse.class);

        if (response == null || response.termCode() == null) {
            throw new IllegalStateException("UCSD catalog API returned an empty or invalid terms response");
        }

        return response.termCode();
    }
}
