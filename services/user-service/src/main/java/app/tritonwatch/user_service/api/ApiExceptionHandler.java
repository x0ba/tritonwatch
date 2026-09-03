package app.tritonwatch.user_service.api;

import app.tritonwatch.user_service.userprofile.UserProfileConflictException;
import app.tritonwatch.user_service.userprofile.UserProfileNotFoundException;
import app.tritonwatch.user_service.verification.InvalidVerificationCodeException;
import app.tritonwatch.user_service.verification.VerificationProviderUnavailableException;
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

    @ExceptionHandler(InvalidVerificationCodeException.class)
    ProblemDetail handleInvalidVerificationCode(InvalidVerificationCodeException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setTitle("Invalid verification code");
        return problem;
    }

    @ExceptionHandler(VerificationProviderUnavailableException.class)
    ProblemDetail handleProviderUnavailable(VerificationProviderUnavailableException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage());
        problem.setTitle("Verification provider unavailable");
        return problem;
    }
}
