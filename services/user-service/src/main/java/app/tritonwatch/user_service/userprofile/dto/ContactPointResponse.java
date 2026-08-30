package app.tritonwatch.user_service.userprofile.dto;

import java.time.Instant;

public record ContactPointResponse(
        String value,
        boolean verified,
        Instant verifiedAt
) {
}
