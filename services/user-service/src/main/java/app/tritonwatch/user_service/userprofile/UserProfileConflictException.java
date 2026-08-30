package app.tritonwatch.user_service.userprofile;

public class UserProfileConflictException extends RuntimeException {

    public UserProfileConflictException(String message) {
        super(message);
    }
}
