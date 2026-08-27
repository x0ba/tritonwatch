package app.tritonwatch.notification_service.subscription;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {
    Optional<Subscription> findByUserIdAndCourseIdAndTerm(UUID userId, String courseId, String term);

    @Modifying
    @Query(value = """
                INSERT INTO subscriptions (
                    id,
                    user_id,
                    course_id,
                    term,
                    created_at,
                    updated_at
                )
                VALUES (
                    :id,
                    :userId,
                    :courseId,
                    :term,
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP
                )
                ON CONFLICT (user_id, course_id, term) DO NOTHING 
            """, nativeQuery = true)
    int insertIfAbsent(@Param("id") UUID id, @Param("userId") UUID userId, @Param("courseId") String courseId, @Param("term") String term);
}
