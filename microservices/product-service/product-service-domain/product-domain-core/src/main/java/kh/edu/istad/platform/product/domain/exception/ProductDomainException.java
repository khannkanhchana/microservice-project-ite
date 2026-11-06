package kh.edu.istad.platform.product.domain.exception;

/**
 * Root exception for all Product domain rule violations.
 */
public class ProductDomainException extends RuntimeException {

    public ProductDomainException(String message) {
        super(message);
    }

    public ProductDomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
