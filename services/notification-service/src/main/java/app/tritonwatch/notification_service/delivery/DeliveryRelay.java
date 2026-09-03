package app.tritonwatch.notification_service.delivery;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeliveryRelay {

    private final DeliveryBatchSender deliveryBatchSender;

    @Scheduled(fixedDelayString = "${notification.delivery.poll-delay-ms:1000}")
    public void relayPendingDeliveries() {
        deliveryBatchSender.sendBatch();
    }
}
