package app.tritonwatch.user_service.verification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class TwilioVerifyClient {

    private final RestClient restClient;
    private final String accountSid;
    private final String authToken;
    private final String verifyServiceSid;

    public TwilioVerifyClient(
            RestClient.Builder restClientBuilder,
            @Value("${verification.twilio.account-sid:}") String accountSid,
            @Value("${verification.twilio.auth-token:}") String authToken,
            @Value("${verification.twilio.verify-service-sid:}") String verifyServiceSid
    ) {
        this.restClient = restClientBuilder.baseUrl("https://verify.twilio.com").build();
        this.accountSid = accountSid == null ? "" : accountSid.trim();
        this.authToken = authToken == null ? "" : authToken.trim();
        this.verifyServiceSid = verifyServiceSid == null ? "" : verifyServiceSid.trim();
    }

    public boolean isConfigured() {
        return !accountSid.isBlank() && !authToken.isBlank() && !verifyServiceSid.isBlank();
    }

    public void startSmsVerification(String phoneE164) {
        if (!isConfigured()) {
            throw new VerificationProviderUnavailableException("SMS verification is not configured");
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("To", phoneE164);
        form.add("Channel", "sms");

        restClient.post()
                .uri("/v2/Services/{serviceSid}/Verifications", verifyServiceSid)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", basicAuthHeader())
                .body(form)
                .retrieve()
                .toBodilessEntity();
    }

    public boolean checkSmsVerification(String phoneE164, String code) {
        if (!isConfigured()) {
            throw new VerificationProviderUnavailableException("SMS verification is not configured");
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("To", phoneE164);
        form.add("Code", code);

        TwilioVerificationCheckResponse response = restClient.post()
                .uri("/v2/Services/{serviceSid}/VerificationCheck", verifyServiceSid)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", basicAuthHeader())
                .body(form)
                .retrieve()
                .body(TwilioVerificationCheckResponse.class);

        return response != null && "approved".equalsIgnoreCase(response.status());
    }

    private String basicAuthHeader() {
        String credentials = Base64.getEncoder()
                .encodeToString((accountSid + ":" + authToken).getBytes(StandardCharsets.UTF_8));
        return "Basic " + credentials;
    }

    private record TwilioVerificationCheckResponse(String status) {
    }
}
