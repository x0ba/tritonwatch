package app.tritonwatch.user_service.verification;

import app.tritonwatch.user_service.userprofile.dto.UserProfileResponse;
import app.tritonwatch.user_service.verification.dto.ConfirmVerificationRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class ContactVerificationController {

    private final ContactVerificationService contactVerificationService;

    @PostMapping("/email/verification-requests")
    public ResponseEntity<Void> requestEmailVerification(@AuthenticationPrincipal Jwt accessToken) {
        contactVerificationService.requestEmailVerification(accessToken.getSubject());
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @PostMapping("/email/verifications")
    public UserProfileResponse confirmEmailVerification(
            @AuthenticationPrincipal Jwt accessToken,
            @RequestBody @Valid ConfirmVerificationRequest request
    ) {
        return contactVerificationService.confirmEmailVerification(accessToken.getSubject(), request.code());
    }

    @PostMapping("/phone/verification-requests")
    public ResponseEntity<Void> requestPhoneVerification(@AuthenticationPrincipal Jwt accessToken) {
        contactVerificationService.requestPhoneVerification(accessToken.getSubject());
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @PostMapping("/phone/verifications")
    public UserProfileResponse confirmPhoneVerification(
            @AuthenticationPrincipal Jwt accessToken,
            @RequestBody @Valid ConfirmVerificationRequest request
    ) {
        return contactVerificationService.confirmPhoneVerification(accessToken.getSubject(), request.code());
    }
}
