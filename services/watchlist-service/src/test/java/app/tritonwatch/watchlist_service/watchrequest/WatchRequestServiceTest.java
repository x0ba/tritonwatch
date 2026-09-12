package app.tritonwatch.watchlist_service.watchrequest;

import app.tritonwatch.watchlist_service.outbox.OutboxEventWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WatchRequestServiceTest {

    @Mock
    private WatchRequestRepository watchRequestRepository;

    @Mock
    private OutboxEventWriter outboxEventWriter;

    @InjectMocks
    private WatchRequestService watchRequestService;

    @Test
    void deleteStopsTrackingWhenLastWatcher() {
        WatchRequest watch = watch("user_student123", "CSE 100", "FA26");
        when(watchRequestRepository.findById(watch.getId())).thenReturn(Optional.of(watch));
        when(watchRequestRepository.findAllByCourseIdAndTerm("CSE 100", "FA26"))
                .thenReturn(List.of(watch));

        watchRequestService.delete("user_student123", watch.getId());

        verify(watchRequestRepository).delete(watch);
        verify(outboxEventWriter).appendWatchDeletedEvents(watch, true);
    }

    @Test
    void deleteKeepsTrackingWhenOtherWatchersRemain() {
        WatchRequest watch = watch("user_student123", "CSE 100", "FA26");
        WatchRequest other = watch("user_other456", "CSE 100", "FA26");
        when(watchRequestRepository.findById(watch.getId())).thenReturn(Optional.of(watch));
        when(watchRequestRepository.findAllByCourseIdAndTerm("CSE 100", "FA26"))
                .thenReturn(List.of(watch, other));

        watchRequestService.delete("user_student123", watch.getId());

        verify(watchRequestRepository).delete(watch);
        verify(outboxEventWriter).appendWatchDeletedEvents(watch, false);
    }

    @Test
    void deleteHidesWatchesOwnedBySomeoneElse() {
        WatchRequest watch = watch("user_other456", "CSE 100", "FA26");
        when(watchRequestRepository.findById(watch.getId())).thenReturn(Optional.of(watch));

        assertThrows(
                WatchRequestNotFoundException.class,
                () -> watchRequestService.delete("user_student123", watch.getId())
        );

        verify(watchRequestRepository, never()).delete(any());
        verify(outboxEventWriter, never()).appendWatchDeletedEvents(any(), anyBoolean());
    }

    @Test
    void deleteThrowsWhenWatchIsMissing() {
        UUID watchRequestId = UUID.randomUUID();
        when(watchRequestRepository.findById(watchRequestId)).thenReturn(Optional.empty());

        assertThrows(
                WatchRequestNotFoundException.class,
                () -> watchRequestService.delete("user_student123", watchRequestId)
        );

        verify(watchRequestRepository, never()).delete(any());
        verify(outboxEventWriter, never()).appendWatchDeletedEvents(any(), anyBoolean());
    }

    private static WatchRequest watch(String userId, String courseId, String term) {
        WatchRequest watchRequest = new WatchRequest();
        watchRequest.setId(UUID.randomUUID());
        watchRequest.setUserId(userId);
        watchRequest.setCourseId(courseId);
        watchRequest.setTerm(term);
        watchRequest.setCreatedAt(Instant.parse("2026-08-30T12:00:00Z"));
        watchRequest.setUpdatedAt(Instant.parse("2026-08-30T12:00:00Z"));
        return watchRequest;
    }
}
