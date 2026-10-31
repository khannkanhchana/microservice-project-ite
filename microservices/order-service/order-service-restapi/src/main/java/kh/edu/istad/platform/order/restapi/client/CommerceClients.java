package kh.edu.istad.platform.order.restapi.client;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import kh.edu.istad.platform.order.application.port.out.CommerceGateway;
import kh.edu.istad.platform.order.application.usecase.CheckoutException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class CommerceClients implements CommerceGateway {
    private final RestClient customers;
    private final RestClient businesses;
    private final RestClient payments;

    public CommerceClients(@Qualifier("commerceRestClientBuilder") RestClient.Builder builder) {
        customers = builder.clone().baseUrl("http://customer-service").build();
        businesses = builder.clone().baseUrl("http://business-service").build();
        payments = builder.clone().baseUrl("http://payment-service").build();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Customer(String status) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CatalogSnapshot(boolean active, List<ProductSnapshot> products) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProductSnapshot(UUID productId, String name, BigDecimal price, int quantity, boolean available) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PaymentSnapshot(UUID paymentId, String status, List<String> failureMessages) {
        PaymentResult result() { return new PaymentResult(paymentId, status, failureMessages); }
    }
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ApprovalSnapshot(String status, List<String> failureMessages) {}

    @Override
    public void requireActiveCustomer(UUID id) {
        var customer = call("Customer", () -> customers.get().uri("/api/customers/{id}", id).retrieve().body(Customer.class));
        if (!"ACTIVE".equals(customer.status()) && !"INACTIVE".equals(customer.status()))
            throw new CheckoutException(CheckoutException.Reason.UNAVAILABLE, "Customer service returned an invalid activity status");
        if (!"ACTIVE".equals(customer.status()))
            throw new CheckoutException(CheckoutException.Reason.CONFLICT, "Customer is inactive");
    }

    @Override
    public Catalog catalog(UUID id) {
        var catalog = call("Business", () -> businesses.get().uri("/api/businesses/{id}", id).retrieve().body(CatalogSnapshot.class));
        if (catalog.products() == null || catalog.products().stream().anyMatch(product -> product == null
                || product.productId() == null || product.name() == null || product.name().isBlank()
                || product.price() == null || product.price().signum() <= 0 || product.quantity() < 0)
                || catalog.products().stream().map(ProductSnapshot::productId).distinct().count() != catalog.products().size())
            throw new CheckoutException(CheckoutException.Reason.UNAVAILABLE, "Business returned an invalid catalog");
        return new Catalog(catalog.active(), catalog.products().stream().map(product -> new CommerceGateway.Product(
                product.productId(), product.name(), product.price(), product.quantity(), product.available())).toList());
    }

    @Override
    public PaymentResult charge(UUID orderId, UUID customerId, BigDecimal amount) {
        var payment = call("Payment", () -> payments.post().uri("/api/payments")
                .body(Map.of("orderId", orderId, "customerId", customerId, "amount", amount))
                .retrieve().body(PaymentSnapshot.class));
        if (payment.paymentId() == null || payment.status() == null)
            throw new CheckoutException(CheckoutException.Reason.UNAVAILABLE, "Payment service returned incomplete payment data");
        return payment.result();
    }

    @Override
    public void refund(UUID id) {
        var result = call("Payment", () -> payments.post().uri("/api/payments/orders/{id}/refund", id)
                .retrieve().body(PaymentSnapshot.class));
        if (!"CANCELLED".equals(result.status()))
            throw new CheckoutException(CheckoutException.Reason.UNAVAILABLE, "Refund has not completed; retry checkout to resume");
    }

    @Override
    public Approval approve(UUID businessId, UUID orderId) {
        var approval = call("Business", () -> businesses.post().uri("/api/businesses/{business}/orders/{order}/approval", businessId, orderId)
                .retrieve().body(ApprovalSnapshot.class));
        if (approval.failureMessages() == null || approval.failureMessages().stream().anyMatch(java.util.Objects::isNull)
                || ("REJECTED".equals(approval.status()) && approval.failureMessages().isEmpty()))
            throw new CheckoutException(CheckoutException.Reason.UNAVAILABLE, "Business returned incomplete approval data");
        return new Approval(approval.status(), approval.failureMessages());
    }

    private <T> T call(String service, Supplier<T> action) {
        try {
            T result = action.get();
            if (result == null) throw new CheckoutException(CheckoutException.Reason.UNAVAILABLE, service + " returned an empty response");
            return result;
        } catch (RestClientResponseException exception) {
            var reason = exception.getStatusCode().value() == 404 ? CheckoutException.Reason.NOT_FOUND
                    : exception.getStatusCode().is4xxClientError() ? CheckoutException.Reason.CONFLICT : CheckoutException.Reason.UNAVAILABLE;
            throw new CheckoutException(reason, service + " service could not fulfill the request");
        } catch (RestClientException exception) {
            throw new CheckoutException(CheckoutException.Reason.UNAVAILABLE, service + " service is temporarily unavailable");
        }
    }

    @Configuration
    static class ClientConfiguration {
        @Bean
        @LoadBalanced
        RestClient.Builder commerceRestClientBuilder() {
            var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build());
            factory.setReadTimeout(Duration.ofSeconds(15));
            return RestClient.builder().requestFactory(factory);
        }
    }
}
