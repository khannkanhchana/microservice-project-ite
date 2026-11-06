package kh.edu.istad.platform.product.dto;

import build.buf.gen.product.ProductResponse;

public record ProductResponseDto(
        String id,
        String name,
        double price,
        int quantity
) {
    public static ProductResponseDto fromGrpcResponse(ProductResponse response) {
        return new ProductResponseDto(
                response.getProductId(),
                response.getName(),
                response.getPrice(),
                response.getQuantity()
        );
    }
}
