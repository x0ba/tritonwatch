package app.tritonwatch.notification_service.delivery;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryAttemptRepository extends JpaRepository<DeliveryAttempt, UUID> {

    @Modifying
    @Query(value = """
            INSERT INTO delivery_attempts (
                id,
                availability_event_id,
                user_id,
                channel,
                course_id,
                term,
                destination,
                open_seat_count,
                open_package_count,
                status,
                attempts,
                created_at,
                updated_at
            )
            VALUES (
                :id,
                :availabilityEventId,
                :userId,
                :channel,
                :courseId,
                :term,
                :destination,
                :openSeatCount,
                :openPackageCount,
                'PENDING',
                0,
                :now,
                :now
            )
            ON CONFLICT (availability_event_id, user_id, channel) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("id") UUID id,
            @Param("availabilityEventId") UUID availabilityEventId,
            @Param("userId") String userId,
            @Param("channel") String channel,
            @Param("courseId") String courseId,
            @Param("term") String term,
            @Param("destination") String destination,
            @Param("openSeatCount") int openSeatCount,
            @Param("openPackageCount") int openPackageCount,
            @Param("now") Instant now
    );

    @Query(value = """
            SELECT id
            FROM delivery_attempts
            WHERE status = 'PENDING'
            ORDER BY created_at, id
            LIMIT :limit
            """, nativeQuery = true)
    List<UUID> findPendingIds(@Param("limit") int limit);

    @Query(value = """
            SELECT *
            FROM delivery_attempts
            WHERE id = :id AND status = 'PENDING'
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    Optional<DeliveryAttempt> lockPendingById(@Param("id") UUID id);
}
