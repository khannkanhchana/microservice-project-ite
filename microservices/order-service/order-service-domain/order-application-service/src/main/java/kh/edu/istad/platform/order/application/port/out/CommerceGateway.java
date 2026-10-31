package kh.edu.istad.platform.order.application.port.out;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** External contracts used by checkout; domain entities do not depend on HTTP clients. */
public interface CommerceGateway {
    void requireActiveCustomer(UUID customerId);
    Catalog catalog(UUID businessId);
    PaymentResult charge(UUID orderId, UUID customerId, BigDecimal amount);
    void refund(UUID orderId);
    Approval approve(UUID businessId, UUID orderId);

    record Catalog(boolean active, List<Product> products) {}
    record Product(UUID productId, String name, BigDecimal price, int quantity, boolean available) {}
    record PaymentResult(UUID paymentId, String status, List<String> failureMessages) {}
    record Approval(String status, List<String> failureMessages) {}
}
