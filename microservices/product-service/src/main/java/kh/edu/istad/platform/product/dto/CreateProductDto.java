package kh.edu.istad.platform.product.dto;

public record CreateProductDto(
        String name,
        double price,
        int quantity
) {

}
