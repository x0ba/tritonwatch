package app.tritonwatch.watchlist_service.watchrequest;

import app.tritonwatch.watchlist_service.outbox.OutboxEventWriter;
import app.tritonwatch.watchlist_service.watchrequest.dto.CreateWatchRequest;
import app.tritonwatch.watchlist_service.watchrequest.dto.CreateWatchResult;
import app.tritonwatch.watchlist_service.watchrequest.dto.WatchRequestResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WatchRequestService {

    private final WatchRequestRepository watchRequestRepository;
    private final OutboxEventWriter outboxEventWriter;

    @Transactional
    public CreateWatchResult create(String userId, CreateWatchRequest request) {

        String normalizedCourseId = request.courseId().trim().toUpperCase(Locale.ROOT);
        String normalizedTerm = request.term().trim().toUpperCase(Locale.ROOT);

        int inserted = watchRequestRepository.insertIfAbsent(
                UUID.randomUUID(),
                userId,
                normalizedCourseId,
                normalizedTerm
        );

        WatchRequest watchRequest = watchRequestRepository
                .findByUserIdAndCourseIdAndTerm(
                        userId,
                        normalizedCourseId,
                        normalizedTerm)
                .orElseThrow(() -> new IllegalStateException("Watch request was not found after creation"));

        if (inserted == 1) {
            outboxEventWriter.appendWatchRequestEvents(watchRequest);
        }

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
