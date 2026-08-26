package app.tritonwatch.watchlist_service.watchrequest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateWatchRequest(@NotBlank @Size(max = 50) String courseId,
                                 @NotBlank @Size(max = 20) String term) {

}
