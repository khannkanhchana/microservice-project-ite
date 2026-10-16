package kh.edu.istad.platform.order.domain.entity;

import kh.edu.istad.platform.order.domain.valueobject.CustomerId;

import java.util.Objects;

public record Customer(CustomerId id) { public Customer { Objects.requireNonNull(id); } }
