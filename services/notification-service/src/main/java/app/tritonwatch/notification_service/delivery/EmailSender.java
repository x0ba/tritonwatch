package app.tritonwatch.notification_service.delivery;

public interface EmailSender {
    boolean isConfigured();

    ProviderSendResult sendCourseAvailable(
            String toEmail,
            String courseId,
            String term,
            int openSeatCount,
            int openPackageCount
    );
}
