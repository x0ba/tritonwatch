package app.tritonwatch.notification_service.usersettings;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class UserNotificationSettingsTest {

    @Test
    void applyIfNewerIgnoresOlderProfileVersions() {
        Instant now = Instant.parse("2026-09-02T19:00:00Z");
        UserNotificationSettings settings = UserNotificationSettings.create(
                "auth0|user",
                3,
                "ACTIVE",
                "a@example.com",
                true,
                null,
                false,
                true,
                false,
                now
        );

        boolean applied = settings.applyIfNewer(
                2,
                "ACTIVE",
                "b@example.com",
                false,
                null,
                false,
                false,
                false,
                now.plusSeconds(10)
        );

        assertThat(applied).isFalse();
        assertThat(settings.getEmail()).isEqualTo("a@example.com");
        assertThat(settings.getProfileVersion()).isEqualTo(3);
    }

    @Test
    void canReceiveEmailRequiresActiveVerifiedEnabledAddress() {
        Instant now = Instant.parse("2026-09-02T19:00:00Z");
        UserNotificationSettings settings = UserNotificationSettings.create(
                "auth0|user",
                1,
                "ACTIVE",
                "a@example.com",
                true,
                null,
                false,
                true,
                false,
                now
        );

        assertThat(settings.canReceiveEmail()).isTrue();
        assertThat(settings.canReceiveSms()).isFalse();
    }
}
