package app.tritonwatch.watchlist_service.watchrequest;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WatchRequestRepository extends JpaRepository<WatchRequest, UUID> {
    Optional<WatchRequest> findByUserIdAndCourseIdAndTerm(String userId, String courseId, String term);

    List<WatchRequest> findByUserIdOrderByCreatedAtDesc(String userId);

    List<WatchRequest> findByUserIdAndTermOrderByCreatedAtDesc(String userId, String term);

    @Modifying
    @Query(value = """
                INSERT INTO watch_requests (
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
    int insertIfAbsent(@Param("id") UUID id, @Param("userId") String userId, @Param("courseId") String courseId, @Param("term") String term);

}
