package app.tritonwatch.notification_service.delivery;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class PostmarkEmailSender implements EmailSender {

    private final RestClient restClient;
    private final String serverToken;
    private final String fromEmail;
    private final String messageStream;

    public PostmarkEmailSender(
            RestClient.Builder restClientBuilder,
            @Value("${notification.postmark.server-token:}") String serverToken,
            @Value("${notification.postmark.from-email:}") String fromEmail,
            @Value("${notification.postmark.message-stream:outbound}") String messageStream
    ) {
        this.restClient = restClientBuilder.baseUrl("https://api.postmarkapp.com").build();
        this.serverToken = serverToken == null ? "" : serverToken.trim();
        this.fromEmail = fromEmail == null ? "" : fromEmail.trim();
        this.messageStream = messageStream;
    }

    @Override
    public boolean isConfigured() {
        return !serverToken.isBlank() && !fromEmail.isBlank();
    }

    @Override
    public ProviderSendResult sendCourseAvailable(
            String toEmail,
            String courseId,
            String term,
            int openSeatCount,
            int openPackageCount
    ) {
        if (!isConfigured()) {
            throw new IllegalStateException("Postmark is not configured");
        }

        String subject = "Seat open: " + courseId + " (" + term + ")";
        String textBody = buildTextBody(courseId, term, openSeatCount, openPackageCount);
        String htmlBody = buildHtmlBody(courseId, term, openSeatCount, openPackageCount);

        PostmarkEmailResponse response = restClient.post()
                .uri("/email")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header("X-Postmark-Server-Token", serverToken)
                .body(Map.of(
                        "From", fromEmail,
                        "To", toEmail,
                        "Subject", subject,
                        "TextBody", textBody,
                        "HtmlBody", htmlBody,
                        "MessageStream", messageStream
                ))
                .retrieve()
                .body(PostmarkEmailResponse.class);

        if (response == null || response.messageId() == null || response.messageId().isBlank()) {
            throw new IllegalStateException("Postmark did not return a MessageID");
        }
        return new ProviderSendResult(response.messageId());
    }

    private record PostmarkEmailResponse(
            @com.fasterxml.jackson.annotation.JsonProperty("MessageID") String messageId
    ) {
    }

    private static String buildTextBody(String courseId, String term, int openSeatCount, int openPackageCount) {
        return """
                A section you are watching is available.

                Course: %s
                Term: %s
                Open seats: %d
                Open packages: %d

                — Tritonwatch
                """.formatted(courseId, term, openSeatCount, openPackageCount).strip();
    }

    private static String buildHtmlBody(String courseId, String term, int openSeatCount, int openPackageCount) {
        return """
                <p>A section you are watching is available.</p>
                <ul>
                  <li><strong>Course:</strong> %s</li>
                  <li><strong>Term:</strong> %s</li>
                  <li><strong>Open seats:</strong> %d</li>
                  <li><strong>Open packages:</strong> %d</li>
                </ul>
                <p>— Tritonwatch</p>
                """.formatted(courseId, term, openSeatCount, openPackageCount).strip();
    }
}
