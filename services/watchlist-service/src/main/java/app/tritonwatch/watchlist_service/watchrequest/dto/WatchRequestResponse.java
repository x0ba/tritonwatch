package app.tritonwatch.watchlist_service.watchrequest.dto;

import java.time.Instant;
import java.util.UUID;

public record WatchRequestResponse(UUID id, String courseId, String term, Instant createdAt) {
}
