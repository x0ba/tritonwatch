package app.tritonwatch.ingestion_service.ucsd.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record CourseSearchRequest(
        @JsonProperty("term_code") String termCode,
        @JsonProperty("course_key") List<String> courseKeys,
        int limit,
        int offset
) {
}
