package app.tritonwatch.user_service.api;

import app.tritonwatch.user_service.userprofile.UserProfileConflictException;
import app.tritonwatch.user_service.userprofile.UserProfileNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(UserProfileNotFoundException.class)
    ProblemDetail handleNotFound(UserProfileNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("User profile not found");
        return problem;
    }

    @ExceptionHandler(UserProfileConflictException.class)
    ProblemDetail handleConflict(UserProfileConflictException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("User profile conflict");
        return problem;
    }
}
