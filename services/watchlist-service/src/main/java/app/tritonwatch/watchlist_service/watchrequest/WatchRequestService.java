package app.tritonwatch.watchlist_service.watchrequest;

import app.tritonwatch.watchlist_service.watchrequest.dto.CreateWatchRequest;
import app.tritonwatch.watchlist_service.watchrequest.dto.CreateWatchResult;
import app.tritonwatch.watchlist_service.watchrequest.dto.WatchRequestResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WatchRequestService {
    private final WatchRequestRepository watchRequestRepository;

    public CreateWatchResult create(UUID userId, CreateWatchRequest request) {
        int inserted = watchRequestRepository.insertIfAbsent(
                UUID.randomUUID(),
                userId,
                request.courseId(),
                request.term()
        );

        WatchRequest watchRequest = watchRequestRepository
                .findByUserIdAndCourseIdAndTerm(
                        userId,
                        request.courseId(),
                        request.term())
                .orElseThrow(() -> new IllegalStateException("Watch request was not found after creation"));

        return new CreateWatchResult(
                toResponse(watchRequest),
                inserted == 1
        );
    }

    private WatchRequestResponse toResponse(WatchRequest watchRequest) {
        return new WatchRequestResponse(
                watchRequest.getId(),
                watchRequest.getCourseId(),
                watchRequest.getTerm(),
                watchRequest.getCreatedAt()
        );
    }

}
