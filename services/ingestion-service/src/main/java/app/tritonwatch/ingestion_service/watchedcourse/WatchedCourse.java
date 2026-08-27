package app.tritonwatch.ingestion_service.watchedcourse;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "watched_courses",
        uniqueConstraints = @UniqueConstraint(columnNames = {"course_id", "term"})
)
public class WatchedCourse {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Size(max = 50)
    @Column(name = "course_id", nullable = false, length = 50)
    private String courseId;

    @NotBlank
    @Size(max = 20)
    @Column(nullable = false, length = 20)
    private String term;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @CreationTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
