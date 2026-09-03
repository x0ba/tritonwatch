package app.tritonwatch.notification_service.delivery;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class TwilioSmsSender implements SmsSender {

    private final RestClient restClient;
    private final String accountSid;
    private final String authToken;
    private final String fromNumber;
    private final String messagingServiceSid;

    public TwilioSmsSender(
            RestClient.Builder restClientBuilder,
            @Value("${notification.twilio.account-sid:}") String accountSid,
            @Value("${notification.twilio.auth-token:}") String authToken,
            @Value("${notification.twilio.from-number:}") String fromNumber,
            @Value("${notification.twilio.messaging-service-sid:}") String messagingServiceSid
    ) {
        this.restClient = restClientBuilder.baseUrl("https://api.twilio.com").build();
        this.accountSid = accountSid == null ? "" : accountSid.trim();
        this.authToken = authToken == null ? "" : authToken.trim();
        this.fromNumber = fromNumber == null ? "" : fromNumber.trim();
        this.messagingServiceSid = messagingServiceSid == null ? "" : messagingServiceSid.trim();
    }

    @Override
    public boolean isConfigured() {
        return !accountSid.isBlank()
                && !authToken.isBlank()
                && (!fromNumber.isBlank() || !messagingServiceSid.isBlank());
    }

    @Override
    public ProviderSendResult sendCourseAvailable(
            String toPhoneE164,
            String courseId,
            String term,
            int openSeatCount,
            int openPackageCount
    ) {
        if (!isConfigured()) {
            throw new IllegalStateException("Twilio is not configured");
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("To", toPhoneE164);
        form.add("Body", buildBody(courseId, term, openSeatCount, openPackageCount));
        if (!messagingServiceSid.isBlank()) {
            form.add("MessagingServiceSid", messagingServiceSid);
        } else {
            form.add("From", fromNumber);
        }

        String credentials = Base64.getEncoder()
                .encodeToString((accountSid + ":" + authToken).getBytes(StandardCharsets.UTF_8));

        TwilioMessageResponse response = restClient.post()
                .uri("/2010-04-01/Accounts/{accountSid}/Messages.json", accountSid)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Basic " + credentials)
                .body(form)
                .retrieve()
                .body(TwilioMessageResponse.class);

        if (response == null || response.sid() == null || response.sid().isBlank()) {
            throw new IllegalStateException("Twilio did not return a message sid");
        }
        return new ProviderSendResult(response.sid());
    }

    private record TwilioMessageResponse(String sid) {
    }

    private static String buildBody(String courseId, String term, int openSeatCount, int openPackageCount) {
        return "Tritonwatch: %s (%s) is open — seats %d, packages %d."
                .formatted(courseId, term, openSeatCount, openPackageCount);
    }
}
