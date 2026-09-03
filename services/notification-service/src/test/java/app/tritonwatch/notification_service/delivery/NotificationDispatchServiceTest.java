package app.tritonwatch.notification_service.delivery;

import app.tritonwatch.contracts.event.CourseSectionBecameAvailable;
import app.tritonwatch.notification_service.subscription.Subscription;
import app.tritonwatch.notification_service.subscription.SubscriptionRepository;
import app.tritonwatch.notification_service.usersettings.UserNotificationSettings;
import app.tritonwatch.notification_service.usersettings.UserNotificationSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationDispatchServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-02T19:00:00Z");

    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private UserNotificationSettingsRepository userNotificationSettingsRepository;
    @Mock
    private DeliveryAttemptRepository deliveryAttemptRepository;

    private NotificationDispatchService notificationDispatchService;

    @BeforeEach
    void setUp() {
        notificationDispatchService = new NotificationDispatchService(
                subscriptionRepository,
                userNotificationSettingsRepository,
                deliveryAttemptRepository,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void enqueueCreatesEmailAndSmsAttemptsForEligibleSubscribers() {
        Subscription subscription = new Subscription();
        subscription.setUserId("auth0|user-1");
        subscription.setCourseId("CSE 100");
        subscription.setTerm("FA26");

        UserNotificationSettings settings = UserNotificationSettings.create(
                "auth0|user-1",
                1,
                "ACTIVE",
                "student@ucsd.edu",
                true,
                "+18585550123",
                true,
                true,
                true,
                NOW
        );

        when(subscriptionRepository.findByCourseIdAndTerm("CSE 100", "FA26"))
                .thenReturn(List.of(subscription));
        when(userNotificationSettingsRepository.findByUserIdIn(List.of("auth0|user-1")))
                .thenReturn(List.of(settings));
        when(deliveryAttemptRepository.insertIfAbsent(
                any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt(), any()
        )).thenReturn(1);

        UUID eventId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        notificationDispatchService.enqueue(new CourseSectionBecameAvailable(
                eventId,
                NOW,
                "cse 100",
                "fa26",
                2,
                0,
                1,
                0
        ));

        ArgumentCaptor<String> channelCaptor = ArgumentCaptor.forClass(String.class);
        verify(deliveryAttemptRepository).insertIfAbsent(
                any(),
                eq(eventId),
                eq("auth0|user-1"),
                channelCaptor.capture(),
                eq("CSE 100"),
                eq("FA26"),
                eq("student@ucsd.edu"),
                eq(2),
                eq(1),
                eq(NOW)
        );
        verify(deliveryAttemptRepository).insertIfAbsent(
                any(),
                eq(eventId),
                eq("auth0|user-1"),
                channelCaptor.capture(),
                eq("CSE 100"),
                eq("FA26"),
                eq("+18585550123"),
                eq(2),
                eq(1),
                eq(NOW)
        );
        assertThat(channelCaptor.getAllValues()).containsExactlyInAnyOrder("EMAIL", "SMS");
    }

    @Test
    void enqueueSkipsUsersWithoutVerifiedChannels() {
        Subscription subscription = new Subscription();
        subscription.setUserId("auth0|user-2");
        subscription.setCourseId("CSE 100");
        subscription.setTerm("FA26");

        UserNotificationSettings settings = UserNotificationSettings.create(
                "auth0|user-2",
                1,
                "ACTIVE",
                "student@ucsd.edu",
                false,
                "+18585550123",
                false,
                true,
                true,
                NOW
        );

        when(subscriptionRepository.findByCourseIdAndTerm("CSE 100", "FA26"))
                .thenReturn(List.of(subscription));
        when(userNotificationSettingsRepository.findByUserIdIn(List.of("auth0|user-2")))
                .thenReturn(List.of(settings));

        notificationDispatchService.enqueue(new CourseSectionBecameAvailable(
                UUID.randomUUID(),
                NOW,
                "CSE 100",
                "FA26",
                1,
                0,
                0,
                0
        ));

        verify(deliveryAttemptRepository, never()).insertIfAbsent(
                any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt(), any()
        );
    }
}
