package app.tritonwatch.contracts.event;

import java.time.Instant;
import java.util.UUID;

public record UserNotificationSettingsUpdated(
        UUID eventId,
        Instant occurredAt,
        String userId,
        long profileVersion,
        String status,
        String email,
        boolean emailVerified,
        String phoneNumberE164,
        boolean phoneVerified,
        boolean emailEnabled,
        boolean smsEnabled
) {
}
