package app.tritonwatch.user_service.verification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ConfirmVerificationRequest(
        @NotBlank
        @Size(min = 4, max = 10)
        @Pattern(regexp = "^[0-9]+$")
        String code
) {
}
