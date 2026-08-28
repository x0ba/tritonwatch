package app.tritonwatch.ingestion_service.ucsd.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CourseSearchResponse(
        int total,
        List<CatalogCourseResponse> courses
) {
}
