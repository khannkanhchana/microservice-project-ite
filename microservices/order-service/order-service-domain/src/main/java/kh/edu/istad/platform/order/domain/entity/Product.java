package kh.edu.istad.platform.order.domain.entity;

import kh.edu.istad.platform.order.domain.exception.DomainException;
import kh.edu.istad.platform.order.domain.valueobject.Money;
import kh.edu.istad.platform.order.domain.valueobject.ProductId;

public record Product(ProductId id, String name, Money price) {
    public Product {
        if (id == null || name == null || name.isBlank() || price == null) throw new DomainException("Invalid product");
        price.requirePositive();
    }
}
