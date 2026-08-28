package app.tritonwatch.ingestion_service.ucsd.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CatalogTermsResponse(@JsonProperty("term_code") String termCode) {

}
