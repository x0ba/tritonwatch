package app.tritonwatch.user_service.userprofile.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserProfileRequest(
        @Size(max = 120) String displayName,
        @Email @Size(max = 320) String email,
        @Pattern(
                regexp = "^\\+[1-9][0-9]{7,14}$",
                message = "must be an E.164 phone number such as +18585550123"
        ) String phoneNumber
) {
}
