package app.tritonwatch.notification_service.usersettings;

import app.tritonwatch.contracts.event.UserNotificationSettingsUpdated;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserNotificationSettingsService {

    private final UserNotificationSettingsRepository userNotificationSettingsRepository;

    @Transactional
    public void upsert(UserNotificationSettingsUpdated event) {
        userNotificationSettingsRepository.findById(event.userId())
                .ifPresentOrElse(
                        existing -> {
                            boolean applied = existing.applyIfNewer(
                                    event.profileVersion(),
                                    event.status(),
                                    event.email(),
                                    event.emailVerified(),
                                    event.phoneNumberE164(),
                                    event.phoneVerified(),
                                    event.emailEnabled(),
                                    event.smsEnabled(),
                                    event.occurredAt()
                            );
                            if (applied) {
                                userNotificationSettingsRepository.save(existing);
                            }
                        },
                        () -> userNotificationSettingsRepository.save(UserNotificationSettings.create(
                                event.userId(),
                                event.profileVersion(),
                                event.status(),
                                event.email(),
                                event.emailVerified(),
                                event.phoneNumberE164(),
                                event.phoneVerified(),
                                event.emailEnabled(),
                                event.smsEnabled(),
                                event.occurredAt()
                        ))
                );
    }
}
