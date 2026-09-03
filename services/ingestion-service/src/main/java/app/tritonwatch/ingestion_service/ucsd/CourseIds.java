package app.tritonwatch.ingestion_service.ucsd;

import app.tritonwatch.ingestion_service.ucsd.dto.CatalogCourseResponse;

import java.util.Locale;

public final class CourseIds {

    private CourseIds() {
    }

    /**
     * UCSD's search API requires {@code SUBJECT COURSE} keys (e.g. {@code CSE 100}).
     * Prefer subject/course codes from the payload; fall back to rewriting {@code module_code}.
     */
    public static String fromCatalogCourse(CatalogCourseResponse course) {
        if (course.subjectCode() != null && !course.subjectCode().isBlank()
                && course.courseCode() != null && !course.courseCode().isBlank()) {
            return normalize(course.subjectCode() + " " + course.courseCode());
        }

        String moduleCode = course.moduleCode();
        if (moduleCode == null || moduleCode.isBlank()) {
            throw new IllegalArgumentException("Catalog course is missing module_code");
        }

        int hyphen = moduleCode.indexOf('-');
        if (hyphen > 0 && hyphen < moduleCode.length() - 1) {
            return normalize(moduleCode.substring(0, hyphen) + " " + moduleCode.substring(hyphen + 1));
        }

        return normalize(moduleCode);
    }

    public static String normalize(String courseId) {
        return courseId.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }
}
