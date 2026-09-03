package app.tritonwatch.ingestion_service.coursecatalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "course_catalog_entries")
@Getter
@Setter
public class CourseCatalogEntry {

    @Id
    private UUID id;

    @NotBlank
    @Size(max = 20)
    @Column(nullable = false, length = 20)
    private String term;

    @NotBlank
    @Size(max = 50)
    @Column(name = "course_id", nullable = false, length = 50)
    private String courseId;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "open_seat_count", nullable = false)
    private int openSeatCount;

    @Column(name = "waitlist_count", nullable = false)
    private int waitlistCount;

    @Column(name = "refreshed_at", nullable = false)
    private Instant refreshedAt;
}
