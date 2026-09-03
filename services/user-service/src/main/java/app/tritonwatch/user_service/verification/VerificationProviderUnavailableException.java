package app.tritonwatch.user_service.verification;

public class VerificationProviderUnavailableException extends RuntimeException {
    public VerificationProviderUnavailableException(String message) {
        super(message);
    }
}
