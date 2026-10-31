package kh.edu.istad.platform.order.domain.entity;

import kh.edu.istad.platform.order.domain.exception.DomainException;
import kh.edu.istad.platform.order.domain.valueobject.Money;

public record Product(
        java.util.UUID productId,
        String name,
        Money price
) {

    public Product {
        if (productId == null) throw new DomainException("Product ID is required");
        if (name == null || name.isBlank()) {
            throw new DomainException("Product name is required");
        }
        if (price == null){
            throw new DomainException("Product price is required");
        }

        name = name.strip();
        price.requirePositive();

    }
}
