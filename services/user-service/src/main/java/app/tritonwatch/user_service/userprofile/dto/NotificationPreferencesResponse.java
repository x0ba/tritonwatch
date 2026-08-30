package app.tritonwatch.user_service.userprofile.dto;

public record NotificationPreferencesResponse(
        boolean emailEnabled,
        boolean smsEnabled,
        boolean effectiveEmailEnabled,
        boolean effectiveSmsEnabled
) {
}
