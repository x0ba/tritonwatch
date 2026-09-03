package app.tritonwatch.ingestion_service.ucsd.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CatalogCourseResponse(
        @JsonProperty("term_code") String termCode,
        @JsonProperty("subject_code") String subjectCode,
        @JsonProperty("course_code") String courseCode,
        @JsonProperty("module_code") String moduleCode,
        @JsonProperty("module_name") String moduleName,
        @JsonProperty("open_seat_count") int openSeatCount,
        @JsonProperty("open_package_count") int openPackageCount,
        @JsonProperty("waitlist_package_count") int waitlistPackageCount,
        @JsonProperty("availability_refresh_pending") boolean availabilityRefreshPending
) {
}
