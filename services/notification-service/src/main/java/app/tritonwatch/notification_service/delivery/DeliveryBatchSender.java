package app.tritonwatch.notification_service.delivery;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeliveryBatchSender {

    private final DeliveryAttemptRepository deliveryAttemptRepository;
    private final DeliveryAttemptSender deliveryAttemptSender;

    @Value("${notification.delivery.batch-size:50}")
    private int batchSize;

    public void sendBatch() {
        for (var attemptId : deliveryAttemptRepository.findPendingIds(batchSize)) {
            if (!deliveryAttemptSender.send(attemptId)) {
                return;
            }
        }
    }
}
