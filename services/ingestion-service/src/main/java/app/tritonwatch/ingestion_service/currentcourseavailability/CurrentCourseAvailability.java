package app.tritonwatch.ingestion_service.currentcourseavailability;

import app.tritonwatch.ingestion_service.ucsd.CourseIds;
import app.tritonwatch.ingestion_service.ucsd.dto.CatalogCourseResponse;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "current_course_availability")
@Getter
@Setter
public class CurrentCourseAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Size(max = 20)
    @Column(nullable = false, length = 20)
    private String term;

    @NotBlank
    @Size(max = 50)
    @Column(nullable = false, length = 50)
    private String courseId;

    @Column(name = "open_seat_count", nullable = false)
    private int openSeatCount;

    @Column(name = "open_package_count", nullable = false)
    private int openPackageCount;

    public static CurrentCourseAvailability from(CatalogCourseResponse course) {
        CurrentCourseAvailability availability = new CurrentCourseAvailability();

        availability.setId(UUID.randomUUID());
        availability.setTerm(course.termCode());
        availability.setCourseId(CourseIds.fromCatalogCourse(course));
        availability.setOpenSeatCount(course.openSeatCount());
        availability.setOpenPackageCount(course.openPackageCount());

        return availability;
    }

}
