package app.tritonwatch.watchlist_service.watchrequest.dto;

import java.util.List;

public record WatchRequestListResponse(List<WatchRequestResponse> watches) {
}
