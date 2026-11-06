package kh.edu.istad.platform.order.application.dto;


public record QueryProductResult(
        String id,
        String name,
        double price,
        int quantity
) {
}