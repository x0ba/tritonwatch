package app.tritonwatch.ingestion_service.ucsd.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PlannerTermsResponse(List<PlannerTerm> terms) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PlannerTerm(
            @JsonProperty("term_code") String termCode,
            @JsonProperty("term_name") String termName,
            @JsonProperty("course_count") Integer courseCount,
            boolean configured
    ) {
    }
}
