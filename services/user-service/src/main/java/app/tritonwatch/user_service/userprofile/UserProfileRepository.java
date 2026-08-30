package app.tritonwatch.user_service.userprofile;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfile, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT profile FROM UserProfile profile WHERE profile.userId = :userId")
    Optional<UserProfile> findByUserIdForUpdate(@Param("userId") String userId);

    @Modifying
    @Query(value = """
            INSERT INTO user_profiles (
                user_id,
                display_name,
                email,
                phone_e164,
                status,
                profile_version,
                created_at,
                updated_at
            )
            VALUES (
                :userId,
                :displayName,
                :email,
                :phoneE164,
                'ACTIVE',
                1,
                :now,
                :now
            )
            ON CONFLICT (user_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("userId") String userId,
            @Param("displayName") String displayName,
            @Param("email") String email,
            @Param("phoneE164") String phoneE164,
            @Param("now") Instant now
    );
}
