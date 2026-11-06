package kh.edu.istad.platform.product.application.dto;

/**
 * Read model / query result DTO for a product.
 * Returned by all application-layer use cases; adapters (gRPC, REST) map this to their own responses.
 */
public record ProductResult(
        String id,
        String name,
        double price,
        int quantity
) {
}
