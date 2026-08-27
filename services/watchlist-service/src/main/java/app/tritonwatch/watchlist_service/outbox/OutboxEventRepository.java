package app.tritonwatch.watchlist_service.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    @Query(value = """
                SELECT event_id
                FROM outbox_events
                WHERE published_at IS NULL
                ORDER BY occurred_at, event_id
                LIMIT :limit
            """, nativeQuery = true)
    List<UUID> findPendingEventIds(@Param("limit") int limit);

    @Query(value = """
                SELECT *
                FROM outbox_events
                WHERE event_id = :eventId
                  AND published_at IS NULL
                FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    Optional<OutboxEvent> lockPendingById(@Param("eventId") UUID eventId);
}
