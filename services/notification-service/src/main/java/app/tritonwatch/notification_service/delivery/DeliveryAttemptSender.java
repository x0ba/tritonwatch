package app.tritonwatch.notification_service.delivery;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryAttemptSender {

    private final DeliveryAttemptRepository deliveryAttemptRepository;
    private final EmailSender emailSender;
    private final SmsSender smsSender;
    private final Clock clock;

    @Value("${notification.delivery.max-attempts:5}")
    private int maxAttempts;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean send(UUID attemptId) {
        return deliveryAttemptRepository.lockPendingById(attemptId)
                .map(this::sendLocked)
                .orElse(true);
    }

    private boolean sendLocked(DeliveryAttempt attempt) {
        Instant now = clock.instant();
        try {
            ProviderSendResult result = switch (attempt.getChannel()) {
                case EMAIL -> emailSender.sendCourseAvailable(
                        attempt.getDestination(),
                        attempt.getCourseId(),
                        attempt.getTerm(),
                        attempt.getOpenSeatCount(),
                        attempt.getOpenPackageCount()
                );
                case SMS -> smsSender.sendCourseAvailable(
                        attempt.getDestination(),
                        attempt.getCourseId(),
                        attempt.getTerm(),
                        attempt.getOpenSeatCount(),
                        attempt.getOpenPackageCount()
                );
            };
            attempt.markSent(result.messageId(), now);
            return true;
        } catch (Exception exception) {
            attempt.markFailed(exception, now, maxAttempts);
            log.warn(
                    "Could not send {} notification attempt {} for user {}",
                    attempt.getChannel(),
                    attempt.getId(),
                    attempt.getUserId(),
                    exception
            );
            return true;
        }
    }
}
