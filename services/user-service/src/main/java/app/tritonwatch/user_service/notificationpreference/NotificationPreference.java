package app.tritonwatch.user_service.notificationpreference;

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
@Table(name = "notification_preferences")
public class NotificationPreference {

    @Id
    @NotBlank
    @Size(max = 255)
    @Column(name = "user_id", nullable = false, updatable = false, length = 255)
    private String userId;

    @Column(name = "email_enabled", nullable = false)
    private boolean emailEnabled;

    @Column(name = "sms_enabled", nullable = false)
    private boolean smsEnabled;

    @NotNull
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static NotificationPreference create(String userId, Instant now) {
        NotificationPreference preference = new NotificationPreference();
        preference.userId = userId;
        preference.updatedAt = now;
        return preference;
    }

    public boolean update(boolean emailEnabled, boolean smsEnabled, Instant now) {
        if (this.emailEnabled == emailEnabled && this.smsEnabled == smsEnabled) {
            return false;
        }

        this.emailEnabled = emailEnabled;
        this.smsEnabled = smsEnabled;
        this.updatedAt = now;
        return true;
    }
}
