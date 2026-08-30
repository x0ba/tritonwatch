package app.tritonwatch.user_service.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxRelay {

    private final OutboxBatchPublisher outboxBatchPublisher;

    @Scheduled(fixedDelayString = "${outbox.poll-delay-ms:1000}")
    public void poll() {
        outboxBatchPublisher.publishBatch();
    }
}
