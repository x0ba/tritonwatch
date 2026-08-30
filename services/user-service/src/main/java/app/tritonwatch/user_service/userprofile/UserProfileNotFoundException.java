package app.tritonwatch.user_service.userprofile;

public class UserProfileNotFoundException extends RuntimeException {

    public UserProfileNotFoundException() {
        super("User profile was not found");
    }
}
