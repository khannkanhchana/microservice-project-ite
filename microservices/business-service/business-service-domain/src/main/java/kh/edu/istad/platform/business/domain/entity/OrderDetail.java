package kh.edu.istad.platform.business.domain.entity;

import kh.edu.istad.platform.business.domain.exception.DomainException;
import kh.edu.istad.platform.business.domain.valueobject.*;

import java.util.List;

public record OrderDetail(OrderId orderId, OrderStatus orderStatus, Money totalAmount, List<Product> products) {
    public OrderDetail {
        if (orderId == null || orderStatus == null || totalAmount == null || products == null || products.isEmpty() || products.stream().anyMatch(p -> p == null))
            throw new DomainException("Order details are required");
        products = List.copyOf(products);
    }
}
