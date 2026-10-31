package kh.edu.istad.platform.business.application.port.out;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import reactor.core.publisher.Mono;

public interface BusinessOperations {
    reactor.core.publisher.Flux<View> list();
    Mono<View> get(UUID businessId);
    Mono<View> setActive(UUID businessId, boolean active);
    Mono<View> putProduct(UUID businessId, UUID productId, CatalogProduct product);
    Mono<Approval> approve(UUID businessId, UUID orderId);

    record View(UUID businessId, String legalName, String displayName, boolean active, List<CatalogProduct> products) {}
    record CatalogProduct(UUID productId, String name, BigDecimal price, int quantity, boolean available) {}
    record Approval(String status, List<String> failureMessages) {}
}
