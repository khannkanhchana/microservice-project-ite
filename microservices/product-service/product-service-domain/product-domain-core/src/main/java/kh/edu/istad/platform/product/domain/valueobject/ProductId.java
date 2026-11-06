package kh.edu.istad.platform.product.domain.valueobject;

import kh.edu.istad.platform.product.domain.exception.ProductDomainException;

import java.util.UUID;

/**
 * Value object wrapping the unique product identifier.
 */
public record ProductId(String id) {

    public ProductId {
        if (id == null || id.isBlank()) {
            throw new ProductDomainException("Product ID is required");
        }
    }

    public static ProductId generate() {
        return new ProductId(UUID.randomUUID().toString());
    }

    public static ProductId of(String id) {
        return new ProductId(id);
    }
}
