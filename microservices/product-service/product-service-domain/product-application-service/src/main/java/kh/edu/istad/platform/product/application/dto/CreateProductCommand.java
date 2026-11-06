package kh.edu.istad.platform.product.application.dto;

/**
 * Command DTO for creating a new product.
 * Crosses the boundary from the REST / gRPC adapter into the application layer.
 */
public record CreateProductCommand(
        String name,
        double price,
        int quantity
) {
}
