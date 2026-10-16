package kh.edu.istad.platform.order.domain.valueobject;

import kh.edu.istad.platform.order.domain.exception.DomainException;

import java.util.UUID;

public record TrackingId(UUID value) {
    public TrackingId { if (value == null) throw new DomainException("TrackingId is required"); }
    public static TrackingId generate() { return new TrackingId(UUID.randomUUID()); }
}
