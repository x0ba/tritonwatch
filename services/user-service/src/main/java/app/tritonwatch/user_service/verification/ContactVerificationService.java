package app.tritonwatch.user_service.verification;

import app.tritonwatch.user_service.notificationpreference.NotificationPreference;
import app.tritonwatch.user_service.notificationpreference.NotificationPreferenceRepository;
import app.tritonwatch.user_service.outbox.OutboxEventWriter;
import app.tritonwatch.user_service.userprofile.UserProfile;
import app.tritonwatch.user_service.userprofile.UserProfileConflictException;
import app.tritonwatch.user_service.userprofile.UserProfileNotFoundException;
import app.tritonwatch.user_service.userprofile.UserProfileRepository;
import app.tritonwatch.user_service.userprofile.UserProfileService;
import app.tritonwatch.user_service.userprofile.dto.UserProfileResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContactVerificationService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserProfileRepository userProfileRepository;
    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final ContactVerificationChallengeRepository challengeRepository;
    private final PostmarkVerificationMailer postmarkVerificationMailer;
    private final TwilioVerifyClient twilioVerifyClient;
    private final OutboxEventWriter outboxEventWriter;
    private final UserProfileService userProfileService;
    private final Clock clock;

    @Value("${verification.email.code-ttl:PT15M}")
    private Duration emailCodeTtl;

    @Transactional
    public void requestEmailVerification(String userId) {
        Instant now = clock.instant();
        UserProfile profile = requireActive(userId);
        if (profile.getEmail() == null) {
            throw new UserProfileConflictException("Add an email address before requesting email verification");
        }
        if (profile.getEmailVerifiedAt() != null) {
            throw new UserProfileConflictException("Email is already verified");
        }

        String code = generateNumericCode(6);
        String codeHash = hashCode(code);
        Instant expiresAt = now.plus(emailCodeTtl);

        challengeRepository.findByUserIdAndChannel(userId, VerificationChannel.EMAIL)
                .ifPresentOrElse(
                        existing -> {
                            existing.refresh(profile.getEmail(), codeHash, expiresAt);
                            challengeRepository.save(existing);
                        },
                        () -> challengeRepository.save(ContactVerificationChallenge.create(
                                UUID.randomUUID(),
                                userId,
                                VerificationChannel.EMAIL,
                                profile.getEmail(),
                                codeHash,
                                expiresAt,
                                now
                        ))
                );

        postmarkVerificationMailer.sendVerificationCode(profile.getEmail(), code);
    }

    @Transactional
    public UserProfileResponse confirmEmailVerification(String userId, String code) {
        Instant now = clock.instant();
        UserProfile profile = requireActive(userId);
        if (profile.getEmail() == null) {
            throw new UserProfileConflictException("Add an email address before confirming email verification");
        }
        if (profile.getEmailVerifiedAt() != null) {
            return userProfileService.get(userId);
        }

        ContactVerificationChallenge challenge = challengeRepository
                .findByUserIdAndChannel(userId, VerificationChannel.EMAIL)
                .orElseThrow(() -> new InvalidVerificationCodeException("No email verification code was requested"));

        if (!challenge.isUsable(now)
                || !challenge.getDestination().equals(profile.getEmail())
                || !constantTimeEquals(challenge.getCodeHash(), hashCode(code))) {
            throw new InvalidVerificationCodeException("Invalid or expired email verification code");
        }

        challenge.consume(now);
        if (profile.markEmailVerified(now)) {
            NotificationPreference preference = requirePreference(userId);
            outboxEventWriter.appendSettingsUpdated(profile, preference, now);
        }
        return userProfileService.get(userId);
    }

    @Transactional
    public void requestPhoneVerification(String userId) {
        UserProfile profile = requireActive(userId);
        if (profile.getPhoneE164() == null) {
            throw new UserProfileConflictException("Add a phone number before requesting SMS verification");
        }
        if (profile.getPhoneVerifiedAt() != null) {
            throw new UserProfileConflictException("Phone number is already verified");
        }
        twilioVerifyClient.startSmsVerification(profile.getPhoneE164());
    }

    @Transactional
    public UserProfileResponse confirmPhoneVerification(String userId, String code) {
        Instant now = clock.instant();
        UserProfile profile = requireActive(userId);
        if (profile.getPhoneE164() == null) {
            throw new UserProfileConflictException("Add a phone number before confirming SMS verification");
        }
        if (profile.getPhoneVerifiedAt() != null) {
            return userProfileService.get(userId);
        }

        if (!twilioVerifyClient.checkSmsVerification(profile.getPhoneE164(), code)) {
            throw new InvalidVerificationCodeException("Invalid or expired SMS verification code");
        }

        if (profile.markPhoneVerified(now)) {
            NotificationPreference preference = requirePreference(userId);
            outboxEventWriter.appendSettingsUpdated(profile, preference, now);
        }
        return userProfileService.get(userId);
    }

    private UserProfile requireActive(String userId) {
        UserProfile profile = userProfileRepository.findByUserIdForUpdate(userId)
                .orElseThrow(UserProfileNotFoundException::new);
        if (!profile.isActive()) {
            throw new UserProfileNotFoundException();
        }
        return profile;
    }

    private NotificationPreference requirePreference(String userId) {
        return notificationPreferenceRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Notification preferences were not found for user " + userId));
    }

    private static String generateNumericCode(int digits) {
        int bound = (int) Math.pow(10, digits);
        int value = SECURE_RANDOM.nextInt(bound / 10, bound);
        return Integer.toString(value);
    }

    private static String hashCode(String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(code.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required", exception);
        }
    }

    private static boolean constantTimeEquals(String left, String right) {
        return MessageDigest.isEqual(
                left.getBytes(StandardCharsets.UTF_8),
                right.getBytes(StandardCharsets.UTF_8)
        );
    }
}
