package app.tritonwatch.watchlist_service.watchrequest;

import app.tritonwatch.watchlist_service.outbox.OutboxEventWriter;
import app.tritonwatch.watchlist_service.watchrequest.dto.CreateWatchRequest;
import app.tritonwatch.watchlist_service.watchrequest.dto.CreateWatchResult;
import app.tritonwatch.watchlist_service.watchrequest.dto.WatchRequestResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
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

    @Transactional
    public void delete(String userId, UUID watchRequestId) {
        WatchRequest watchRequest = watchRequestRepository.findById(watchRequestId)
                .orElseThrow(WatchRequestNotFoundException::new);

        if (!watchRequest.getUserId().equals(userId)) {
            throw new WatchRequestNotFoundException();
        }

        List<WatchRequest> lockedWatches = watchRequestRepository.findAllByCourseIdAndTerm(
                watchRequest.getCourseId(),
                watchRequest.getTerm()
        );

        WatchRequest lockedWatch = lockedWatches.stream()
                .filter(candidate -> candidate.getId().equals(watchRequestId))
                .findFirst()
                .orElseThrow(WatchRequestNotFoundException::new);

        watchRequestRepository.delete(lockedWatch);
        outboxEventWriter.appendWatchDeletedEvents(lockedWatch, lockedWatches.size() == 1);
    }

    public List<WatchRequestResponse> list(String userId, String term) {
        List<WatchRequest> watchRequests = term == null || term.isBlank()
                ? watchRequestRepository.findByUserIdOrderByCreatedAtDesc(userId)
                : watchRequestRepository.findByUserIdAndTermOrderByCreatedAtDesc(
                        userId,
                        term.trim().toUpperCase(Locale.ROOT)
                );

        return watchRequests.stream()
                .map(this::toResponse)
                .toList();
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
