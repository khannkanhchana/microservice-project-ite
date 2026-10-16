package kh.edu.istad.platform.order.domain.entity;

import kh.edu.istad.platform.order.domain.valueobject.BusinessId;

import java.util.List;
import java.util.Objects;

public record Business(BusinessId id, List<Product> products, boolean active) {
    public Business { Objects.requireNonNull(id); products = List.copyOf(products); }
}
