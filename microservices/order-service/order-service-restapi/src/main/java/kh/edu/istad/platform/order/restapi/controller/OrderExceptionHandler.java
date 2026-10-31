package kh.edu.istad.platform.order.restapi.controller;

import kh.edu.istad.platform.order.application.usecase.CheckoutException;
import kh.edu.istad.platform.order.domain.exception.DomainException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class OrderExceptionHandler {
    @ExceptionHandler(org.springframework.dao.TransientDataAccessException.class)
    public ResponseEntity<ProblemDetail> temporaryDatabaseFailure(org.springframework.dao.TransientDataAccessException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE, "Order storage is temporarily unavailable; retry the same checkout request"));
    }

    @ExceptionHandler(CheckoutException.class)
    public ResponseEntity<ProblemDetail> checkout(CheckoutException exception) {
        HttpStatus status = switch (exception.reason()) {
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case INVALID -> HttpStatus.BAD_REQUEST;
            case CONFLICT -> HttpStatus.CONFLICT;
            case UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
        };
        return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, exception.getMessage()));
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ProblemDetail> domain(DomainException exception) {
        var status = exception.getMessage().startsWith("Order not found") ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, exception.getMessage()));
    }
}
