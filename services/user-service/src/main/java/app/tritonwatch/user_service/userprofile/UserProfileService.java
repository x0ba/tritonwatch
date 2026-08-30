package app.tritonwatch.user_service.userprofile;

import app.tritonwatch.user_service.consent.SmsConsent;
import app.tritonwatch.user_service.consent.SmsConsentAction;
import app.tritonwatch.user_service.consent.SmsConsentRepository;
import app.tritonwatch.user_service.notificationpreference.NotificationPreference;
import app.tritonwatch.user_service.notificationpreference.NotificationPreferenceRepository;
import app.tritonwatch.user_service.outbox.OutboxEventWriter;
import app.tritonwatch.user_service.userprofile.dto.ContactPointResponse;
import app.tritonwatch.user_service.userprofile.dto.NotificationPreferencesResponse;
import app.tritonwatch.user_service.userprofile.dto.UpdateNotificationPreferencesRequest;
import app.tritonwatch.user_service.userprofile.dto.UpdateUserProfileRequest;
import app.tritonwatch.user_service.userprofile.dto.UpsertUserProfileResult;
import app.tritonwatch.user_service.userprofile.dto.UserProfileResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private static final String PREFERENCE_API_POLICY_VERSION = "preference-api-v1";

    private final UserProfileRepository userProfileRepository;
    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final SmsConsentRepository smsConsentRepository;
    private final OutboxEventWriter outboxEventWriter;
    private final Clock clock;

    @Transactional
    public UserProfileResponse get(String userId) {
        UserProfile profile = requireActive(userProfileRepository.findById(userId)
                .orElseThrow(UserProfileNotFoundException::new));
        NotificationPreference preference = requirePreference(userId);
        return toResponse(profile, preference);
    }

    @Transactional
    public UpsertUserProfileResult upsert(String userId, UpdateUserProfileRequest request) {
        Instant now = clock.instant();
        String displayName = normalizeOptional(request.displayName());
        String email = normalizeEmail(request.email());
        String phoneE164 = normalizeOptional(request.phoneNumber());

        int inserted = userProfileRepository.insertIfAbsent(
                userId,
                displayName,
                email,
                phoneE164,
                now
        );
        notificationPreferenceRepository.insertIfAbsent(userId, now);

        UserProfile profile = requireActive(userProfileRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new IllegalStateException("User profile was not found after upsert")));
        NotificationPreference preference = requirePreference(userId);

        if (inserted == 1) {
            outboxEventWriter.appendSettingsUpdated(profile, preference, now);
            return new UpsertUserProfileResult(toResponse(profile, preference), true);
        }

        String previousPhone = profile.getPhoneE164();
        boolean phoneChanged = !Objects.equals(previousPhone, phoneE164);
        boolean changed = profile.update(displayName, email, phoneE164, now);

        if (phoneChanged && preference.isSmsEnabled()) {
            preference.update(preference.isEmailEnabled(), false, now);
            recordSmsConsent(
                    userId,
                    previousPhone,
                    SmsConsentAction.OPT_OUT,
                    "CONTACT_CHANGE",
                    PREFERENCE_API_POLICY_VERSION,
                    now
            );
        }

        if (changed) {
            outboxEventWriter.appendSettingsUpdated(profile, preference, now);
        }

        return new UpsertUserProfileResult(toResponse(profile, preference), false);
    }

    @Transactional
    public UserProfileResponse updateNotificationPreferences(
            String userId,
            UpdateNotificationPreferencesRequest request
    ) {
        Instant now = clock.instant();
        UserProfile profile = requireActive(userProfileRepository.findByUserIdForUpdate(userId)
                .orElseThrow(UserProfileNotFoundException::new));
        NotificationPreference preference = requirePreference(userId);

        boolean emailEnabled = request.emailEnabled();
        boolean smsEnabled = request.smsEnabled();

        if (emailEnabled && profile.getEmail() == null) {
            throw new UserProfileConflictException("A contact email is required before email notifications can be enabled");
        }
        if (smsEnabled && profile.getPhoneE164() == null) {
            throw new UserProfileConflictException("A phone number is required before SMS notifications can be enabled");
        }

        boolean enablingSms = smsEnabled && !preference.isSmsEnabled();
        if (enablingSms) {
            if (!Boolean.TRUE.equals(request.smsConsentAccepted())) {
                throw new UserProfileConflictException("SMS consent must be accepted before SMS notifications can be enabled");
            }
            String policyVersion = normalizeOptional(request.smsConsentPolicyVersion());
            if (policyVersion == null) {
                throw new UserProfileConflictException("An SMS consent policy version is required");
            }
            recordSmsConsent(
                    userId,
                    profile.getPhoneE164(),
                    SmsConsentAction.OPT_IN,
                    "PREFERENCE_API",
                    policyVersion,
                    now
            );
        }

        boolean disablingSms = !smsEnabled && preference.isSmsEnabled();
        if (disablingSms) {
            recordSmsConsent(
                    userId,
                    profile.getPhoneE164(),
                    SmsConsentAction.OPT_OUT,
                    "PREFERENCE_API",
                    PREFERENCE_API_POLICY_VERSION,
                    now
            );
        }

        if (preference.update(emailEnabled, smsEnabled, now)) {
            profile.touch(now);
            outboxEventWriter.appendSettingsUpdated(profile, preference, now);
        }

        return toResponse(profile, preference);
    }

    @Transactional
    public void delete(String userId) {
        UserProfile profile = userProfileRepository.findByUserIdForUpdate(userId).orElse(null);
        if (profile == null || profile.getStatus() == UserStatus.DELETED) {
            return;
        }

        Instant now = clock.instant();
        NotificationPreference preference = requirePreference(userId);
        if (preference.isSmsEnabled()) {
            recordSmsConsent(
                    userId,
                    profile.getPhoneE164(),
                    SmsConsentAction.OPT_OUT,
                    "ACCOUNT_DELETION",
                    PREFERENCE_API_POLICY_VERSION,
                    now
            );
        }

        preference.update(false, false, now);
        profile.markDeleted(now);
        outboxEventWriter.appendSettingsUpdated(profile, preference, now);
    }

    private UserProfile requireActive(UserProfile profile) {
        if (!profile.isActive()) {
            throw new UserProfileNotFoundException();
        }
        return profile;
    }

    private NotificationPreference requirePreference(String userId) {
        return notificationPreferenceRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Notification preferences were not found for user " + userId));
    }

    private void recordSmsConsent(
            String userId,
            String phoneE164,
            SmsConsentAction action,
            String source,
            String policyVersion,
            Instant occurredAt
    ) {
        if (phoneE164 == null) {
            return;
        }
        smsConsentRepository.save(SmsConsent.record(
                userId,
                phoneE164,
                action,
                source,
                policyVersion,
                occurredAt
        ));
    }

    private UserProfileResponse toResponse(UserProfile profile, NotificationPreference preference) {
        boolean active = profile.isActive();
        boolean emailVerified = profile.getEmailVerifiedAt() != null;
        boolean phoneVerified = profile.getPhoneVerifiedAt() != null;

        return new UserProfileResponse(
                profile.getUserId(),
                profile.getDisplayName(),
                new ContactPointResponse(profile.getEmail(), emailVerified, profile.getEmailVerifiedAt()),
                new ContactPointResponse(profile.getPhoneE164(), phoneVerified, profile.getPhoneVerifiedAt()),
                new NotificationPreferencesResponse(
                        preference.isEmailEnabled(),
                        preference.isSmsEnabled(),
                        active && preference.isEmailEnabled() && emailVerified,
                        active && preference.isSmsEnabled() && phoneVerified
                ),
                profile.getStatus().name(),
                profile.getProfileVersion(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }

    private String normalizeEmail(String value) {
        String normalized = normalizeOptional(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
