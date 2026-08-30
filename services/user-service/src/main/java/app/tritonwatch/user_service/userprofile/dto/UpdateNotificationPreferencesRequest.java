package app.tritonwatch.user_service.userprofile.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateNotificationPreferencesRequest(
        @NotNull Boolean emailEnabled,
        @NotNull Boolean smsEnabled,
        Boolean smsConsentAccepted,
        @Size(max = 40) String smsConsentPolicyVersion
) {
}
