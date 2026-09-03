package app.tritonwatch.ingestion_service.coursecatalog.dto;

public record CourseCatalogItemResponse(
        String courseId,
        String title,
        int openSeats,
        int waitlist
) {
}
