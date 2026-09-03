package app.tritonwatch.user_service.verification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class PostmarkVerificationMailer {

    private final RestClient restClient;
    private final String serverToken;
    private final String fromEmail;
    private final String messageStream;

    public PostmarkVerificationMailer(
            RestClient.Builder restClientBuilder,
            @Value("${verification.postmark.server-token:}") String serverToken,
            @Value("${verification.postmark.from-email:}") String fromEmail,
            @Value("${verification.postmark.message-stream:outbound}") String messageStream
    ) {
        this.restClient = restClientBuilder.baseUrl("https://api.postmarkapp.com").build();
        this.serverToken = serverToken == null ? "" : serverToken.trim();
        this.fromEmail = fromEmail == null ? "" : fromEmail.trim();
        this.messageStream = messageStream;
    }

    public boolean isConfigured() {
        return !serverToken.isBlank() && !fromEmail.isBlank();
    }

    public void sendVerificationCode(String toEmail, String code) {
        if (!isConfigured()) {
            throw new VerificationProviderUnavailableException("Email verification is not configured");
        }

        restClient.post()
                .uri("/email")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header("X-Postmark-Server-Token", serverToken)
                .body(Map.of(
                        "From", fromEmail,
                        "To", toEmail,
                        "Subject", "Verify your Tritonwatch email",
                        "TextBody", "Your Tritonwatch verification code is " + code + ". It expires in 15 minutes.",
                        "HtmlBody", "<p>Your Tritonwatch verification code is <strong>" + code
                                + "</strong>.</p><p>It expires in 15 minutes.</p>",
                        "MessageStream", messageStream
                ))
                .retrieve()
                .toBodilessEntity();
    }
}
