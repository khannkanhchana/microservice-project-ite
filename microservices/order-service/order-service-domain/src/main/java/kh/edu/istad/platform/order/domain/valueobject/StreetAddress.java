package kh.edu.istad.platform.order.domain.valueobject;

import kh.edu.istad.platform.order.domain.exception.DomainException;

import java.util.UUID;

public record StreetAddress(UUID id, String street, String postalCode, String city) {
    public StreetAddress {
        if (id == null || street == null || street.isBlank() || postalCode == null || postalCode.isBlank() || city == null || city.isBlank())
            throw new DomainException("Complete delivery address is required");
    }
}
