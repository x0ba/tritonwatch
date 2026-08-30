package app.tritonwatch.user_service.userprofile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "user_profiles")
public class UserProfile {

    @Id
    @NotBlank
    @Size(max = 255)
    @Column(name = "user_id", nullable = false, updatable = false, length = 255)
    private String userId;

    @Size(max = 120)
    @Column(name = "display_name", length = 120)
    private String displayName;

    @Email
    @Size(max = 320)
    @Column(length = 320)
    private String email;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    @Pattern(regexp = "^\\+[1-9][0-9]{7,14}$")
    @Size(max = 16)
    @Column(name = "phone_e164", length = 16)
    private String phoneE164;

    @Column(name = "phone_verified_at")
    private Instant phoneVerifiedAt;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    @Column(name = "profile_version", nullable = false)
    private long profileVersion;

    @NotNull
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @NotNull
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static UserProfile create(
            String userId,
            String displayName,
            String email,
            String phoneE164,
            Instant now
    ) {
        UserProfile profile = new UserProfile();
        profile.userId = userId;
        profile.displayName = displayName;
        profile.email = email;
        profile.phoneE164 = phoneE164;
        profile.status = UserStatus.ACTIVE;
        profile.profileVersion = 1;
        profile.createdAt = now;
        profile.updatedAt = now;
        return profile;
    }

    public boolean update(String displayName, String email, String phoneE164, Instant now) {
        boolean changed = !Objects.equals(this.displayName, displayName)
                || !Objects.equals(this.email, email)
                || !Objects.equals(this.phoneE164, phoneE164);

        if (!changed) {
            return false;
        }

        if (!Objects.equals(this.email, email)) {
            this.emailVerifiedAt = null;
        }
        if (!Objects.equals(this.phoneE164, phoneE164)) {
            this.phoneVerifiedAt = null;
        }

        this.displayName = displayName;
        this.email = email;
        this.phoneE164 = phoneE164;
        touch(now);
        return true;
    }

    public void touch(Instant now) {
        this.profileVersion++;
        this.updatedAt = now;
    }

    public void markDeleted(Instant now) {
        this.displayName = null;
        this.email = null;
        this.emailVerifiedAt = null;
        this.phoneE164 = null;
        this.phoneVerifiedAt = null;
        this.status = UserStatus.DELETED;
        touch(now);
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }
}
