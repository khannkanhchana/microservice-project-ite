package kh.edu.istad.platform.order.application.usecase;

public class CheckoutException extends RuntimeException {
    public enum Reason { NOT_FOUND, INVALID, CONFLICT, UNAVAILABLE }
    private final Reason reason;

    public CheckoutException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason reason() { return reason; }
}
