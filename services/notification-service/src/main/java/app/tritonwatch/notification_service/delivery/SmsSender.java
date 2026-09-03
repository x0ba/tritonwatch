package app.tritonwatch.notification_service.delivery;

public interface SmsSender {
    boolean isConfigured();

    ProviderSendResult sendCourseAvailable(
            String toPhoneE164,
            String courseId,
            String term,
            int openSeatCount,
            int openPackageCount
    );
}
