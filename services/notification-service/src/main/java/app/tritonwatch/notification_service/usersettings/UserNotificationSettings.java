package app.tritonwatch.notification_service.usersettings;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "user_notification_settings")
public class UserNotificationSettings {

    @Id
    @NotBlank
    @Size(max = 255)
    @Column(name = "user_id", nullable = false, updatable = false, length = 255)
    private String userId;

    @Column(name = "profile_version", nullable = false)
    private long profileVersion;

    @NotBlank
    @Size(max = 20)
    @Column(nullable = false, length = 20)
    private String status;

    @Size(max = 320)
    @Column(length = 320)
    private String email;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Size(max = 16)
    @Column(name = "phone_e164", length = 16)
    private String phoneE164;

    @Column(name = "phone_verified", nullable = false)
    private boolean phoneVerified;

    @Column(name = "email_enabled", nullable = false)
    private boolean emailEnabled;

    @Column(name = "sms_enabled", nullable = false)
    private boolean smsEnabled;

    @NotNull
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static UserNotificationSettings create(
            String userId,
            long profileVersion,
            String status,
            String email,
            boolean emailVerified,
            String phoneE164,
            boolean phoneVerified,
            boolean emailEnabled,
            boolean smsEnabled,
            Instant updatedAt
    ) {
        UserNotificationSettings settings = new UserNotificationSettings();
        settings.userId = userId;
        settings.apply(
                profileVersion,
                status,
                email,
                emailVerified,
                phoneE164,
                phoneVerified,
                emailEnabled,
                smsEnabled,
                updatedAt
        );
        return settings;
    }

    public boolean applyIfNewer(
            long profileVersion,
            String status,
            String email,
            boolean emailVerified,
            String phoneE164,
            boolean phoneVerified,
            boolean emailEnabled,
            boolean smsEnabled,
            Instant updatedAt
    ) {
        if (profileVersion < this.profileVersion) {
            return false;
        }
        apply(
                profileVersion,
                status,
                email,
                emailVerified,
                phoneE164,
                phoneVerified,
                emailEnabled,
                smsEnabled,
                updatedAt
        );
        return true;
    }

    public boolean isActive() {
        return "ACTIVE".equals(status);
    }

    public boolean canReceiveEmail() {
        return isActive() && emailEnabled && emailVerified && email != null && !email.isBlank();
    }

    public boolean canReceiveSms() {
        return isActive() && smsEnabled && phoneVerified && phoneE164 != null && !phoneE164.isBlank();
    }

    private void apply(
            long profileVersion,
            String status,
            String email,
            boolean emailVerified,
            String phoneE164,
            boolean phoneVerified,
            boolean emailEnabled,
            boolean smsEnabled,
            Instant updatedAt
    ) {
        this.profileVersion = profileVersion;
        this.status = status;
        this.email = email;
        this.emailVerified = emailVerified;
        this.phoneE164 = phoneE164;
        this.phoneVerified = phoneVerified;
        this.emailEnabled = emailEnabled;
        this.smsEnabled = smsEnabled;
        this.updatedAt = updatedAt;
    }
}
