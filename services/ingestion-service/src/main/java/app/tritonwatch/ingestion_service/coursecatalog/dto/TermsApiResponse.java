package app.tritonwatch.ingestion_service.coursecatalog.dto;

import java.util.List;

public record TermsApiResponse(
        String currentTerm,
        List<TermOptionResponse> terms
) {
}
