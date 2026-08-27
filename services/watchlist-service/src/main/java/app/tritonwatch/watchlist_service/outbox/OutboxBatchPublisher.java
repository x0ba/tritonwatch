package app.tritonwatch.watchlist_service.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OutboxBatchPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxSingleEventPublisher outboxSingleEventPublisher;

    @Value("${outbox.batch-size:50}")
    private int batchSize;

    public void publishBatch() {
        for (var eventId : outboxEventRepository.findPendingEventIds(batchSize)) {
            if (!outboxSingleEventPublisher.publish(eventId)) {
                break;
            }
        }
    }
}
