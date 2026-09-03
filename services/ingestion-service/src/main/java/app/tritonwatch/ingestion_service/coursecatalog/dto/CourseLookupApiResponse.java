package app.tritonwatch.ingestion_service.coursecatalog.dto;

import java.util.List;

public record CourseLookupApiResponse(
        String term,
        int count,
        List<CourseCatalogItemResponse> courses
) {
}
