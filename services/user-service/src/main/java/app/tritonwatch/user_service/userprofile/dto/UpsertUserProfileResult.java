package app.tritonwatch.user_service.userprofile.dto;

public record UpsertUserProfileResult(UserProfileResponse profile, boolean created) {
}
