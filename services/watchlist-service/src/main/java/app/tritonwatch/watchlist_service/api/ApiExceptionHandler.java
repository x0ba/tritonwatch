package app.tritonwatch.watchlist_service.api;

import app.tritonwatch.watchlist_service.watchrequest.WatchRequestNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(WatchRequestNotFoundException.class)
    ProblemDetail handleNotFound(WatchRequestNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Watch request not found");
        return problem;
    }
}
