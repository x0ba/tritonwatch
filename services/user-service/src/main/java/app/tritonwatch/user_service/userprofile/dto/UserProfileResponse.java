package app.tritonwatch.user_service.userprofile.dto;

import java.time.Instant;

public record UserProfileResponse(
        String userId,
        String displayName,
        ContactPointResponse email,
        ContactPointResponse phone,
        NotificationPreferencesResponse notificationPreferences,
        String status,
        long version,
        Instant createdAt,
        Instant updatedAt
) {
}
