package app.tritonwatch.user_service.notificationpreference;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, String> {

    @Modifying
    @Query(value = """
            INSERT INTO notification_preferences (
                user_id,
                email_enabled,
                sms_enabled,
                updated_at
            )
            VALUES (:userId, FALSE, FALSE, :now)
            ON CONFLICT (user_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("userId") String userId, @Param("now") Instant now);
}
