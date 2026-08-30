package app.tritonwatch.user_service.userprofile;

import app.tritonwatch.user_service.userprofile.dto.UpdateNotificationPreferencesRequest;
import app.tritonwatch.user_service.userprofile.dto.UpdateUserProfileRequest;
import app.tritonwatch.user_service.userprofile.dto.UpsertUserProfileResult;
import app.tritonwatch.user_service.userprofile.dto.UserProfileResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    @GetMapping
    public UserProfileResponse get(@AuthenticationPrincipal Jwt accessToken) {
        return userProfileService.get(accessToken.getSubject());
    }

    @PutMapping
    public ResponseEntity<UserProfileResponse> upsert(
            @AuthenticationPrincipal Jwt accessToken,
            @RequestBody @Valid UpdateUserProfileRequest request
    ) {
        UpsertUserProfileResult result = userProfileService.upsert(accessToken.getSubject(), request);
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.profile());
    }

    @PutMapping("/notification-preferences")
    public UserProfileResponse updateNotificationPreferences(
            @AuthenticationPrincipal Jwt accessToken,
            @RequestBody @Valid UpdateNotificationPreferencesRequest request
    ) {
        return userProfileService.updateNotificationPreferences(accessToken.getSubject(), request);
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt accessToken) {
        userProfileService.delete(accessToken.getSubject());
        return ResponseEntity.noContent().build();
    }
}
