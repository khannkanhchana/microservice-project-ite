package kh.edu.istad.platform.payment.restapi.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/** Translate dependency failures into stable HTTP responses without exposing upstream details. */
@RestControllerAdvice(assignableTypes = {PaymentController.class, CreditAccountController.class})
public class DependencyExceptionHandler {
    @ExceptionHandler(org.springframework.dao.DataAccessException.class)
    public ResponseEntity<ProblemDetail> storageFailure(org.springframework.dao.DataAccessException exception) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "Payment storage could not complete the request; retry with the same order ID");
    }

    @ExceptionHandler(WebClientRequestException.class)
    public ResponseEntity<ProblemDetail> connectionFailure(WebClientRequestException exception) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, service(exception.getUri()) + " service is temporarily unavailable");
    }

    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<ProblemDetail> upstreamFailure(WebClientResponseException exception) {
        String resource = exception.getRequest() == null ? "Order" : service(exception.getRequest().getURI());
        if (exception.getStatusCode().value() == 404) {
            return problem(HttpStatus.NOT_FOUND, resource + " not found");
        }
        if (exception.getStatusCode().is5xxServerError()) {
            return problem(HttpStatus.SERVICE_UNAVAILABLE, resource + " service is temporarily unavailable");
        }
        return problem(HttpStatus.BAD_GATEWAY, resource + " service rejected the request");
    }

    @ExceptionHandler(kh.edu.istad.platform.payment.domain.exception.DomainException.class)
    public ResponseEntity<ProblemDetail> domain(kh.edu.istad.platform.payment.domain.exception.DomainException exception) {
        return problem(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> status(org.springframework.web.server.ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode()).body(ProblemDetail.forStatusAndDetail(
                exception.getStatusCode(), exception.getReason() == null ? "Request could not be fulfilled" : exception.getReason()));
    }

    private String service(java.net.URI uri) {
        return "customer-service".equals(uri.getHost()) ? "Customer" : "Order";
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail) {
        return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, detail));
    }
}
