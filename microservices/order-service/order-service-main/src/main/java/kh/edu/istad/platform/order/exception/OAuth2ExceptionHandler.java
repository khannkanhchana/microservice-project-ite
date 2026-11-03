package kh.edu.istad.platform.order.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class OAuth2ExceptionHandler {
    @ExceptionHandler(OAuth2AuthorizationException.class)
    ProblemDetail tokenAcquisitionFailed(OAuth2AuthorizationException exception) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "Unable to obtain a service access token. Try again later.");
        problem.setTitle("Service authorization unavailable");
        return problem;
    }
}
