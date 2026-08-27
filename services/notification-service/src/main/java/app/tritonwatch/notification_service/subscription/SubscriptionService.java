package app.tritonwatch.notification_service.subscription;

import app.tritonwatch.contracts.event.UserCourseWatchCreated;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;

    @Transactional
    public void create(UserCourseWatchCreated event) {
        subscriptionRepository.insertIfAbsent(
                UUID.randomUUID(),
                event.userId(),
                event.courseId().trim().toUpperCase(Locale.ROOT),
                event.term().trim().toUpperCase(Locale.ROOT)
        );
    }
}
