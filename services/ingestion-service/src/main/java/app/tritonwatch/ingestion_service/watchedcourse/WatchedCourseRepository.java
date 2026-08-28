package app.tritonwatch.ingestion_service.watchedcourse;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface WatchedCourseRepository extends JpaRepository<WatchedCourse, UUID> {

    List<WatchedCourse> findAllByTerm(String term);

    @Modifying
    @Query(value = """
                INSERT INTO watched_courses (
                    id,
                    course_id,
                    term,
                    created_at,
                    updated_at
                )
                VALUES (
                    :id,
                    :courseId,
                    :term,
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP
                )
                ON CONFLICT (course_id, term) DO NOTHING
            """, nativeQuery = true)
    void insertIfAbsent(@Param("id") UUID id, @Param("courseId") String courseId, @Param("term") String term);
}
