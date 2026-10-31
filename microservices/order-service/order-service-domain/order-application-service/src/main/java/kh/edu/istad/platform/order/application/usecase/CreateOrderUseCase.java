package kh.edu.istad.platform.order.application.usecase;

import kh.edu.istad.platform.order.application.dto.CreateOrderCommand;
import kh.edu.istad.platform.order.application.dto.CreateOrderResult;
import kh.edu.istad.platform.order.application.port.out.OrderRepository;
import kh.edu.istad.platform.order.application.port.out.CommerceGateway;
import kh.edu.istad.platform.order.domain.entity.Order;
import kh.edu.istad.platform.order.domain.entity.OrderItem;
import kh.edu.istad.platform.order.domain.entity.Product;
import kh.edu.istad.platform.order.domain.exception.DomainException;
import kh.edu.istad.platform.order.domain.valueobject.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateOrderUseCase {
    private final OrderRepository orderRepository;
    private final CommerceGateway commerce;

    public CreateOrderResult execute(CreateOrderCommand command){
        if (command == null){
            throw  new DomainException("Order command is required");
        }

        if (command.items() == null || command.items().isEmpty()){
            throw new DomainException("At least one order item is required");
        }

        if (command.items().stream().anyMatch(item -> item == null)){
            throw new DomainException("Order items cannot contain null");
        }

        var orderId = OrderId.generate();

        commerce.requireActiveCustomer(command.customerId());
        var catalog = commerce.catalog(command.businessId());
        if (!catalog.active()) throw new CheckoutException(CheckoutException.Reason.CONFLICT, "Business is inactive");
        var products = catalog.products().stream().collect(java.util.stream.Collectors.toMap(
                CommerceGateway.Product::productId, java.util.function.Function.identity()));
        var seen = new java.util.HashSet<UUID>();
        for (var requested : command.items()) {
            if (!seen.add(requested.productId()))
                throw new CheckoutException(CheckoutException.Reason.INVALID, "Duplicate product");
            var actual = products.get(requested.productId());
            if (actual == null) throw new CheckoutException(CheckoutException.Reason.INVALID, "Product is not in this business catalog");
            if (!actual.available() || requested.quantity() > actual.quantity())
                throw new CheckoutException(CheckoutException.Reason.CONFLICT, "Product is unavailable or has insufficient stock");
            if (actual.price().compareTo(requested.price()) != 0)
                throw new CheckoutException(CheckoutException.Reason.INVALID, "Product price does not match catalog");
        }

        var address = new StreetAddress(
                UUID.randomUUID(),
                command.street(),
                command.postalCode(),
                command.city()
        );

        var items = command
                .items()
                .stream()
                .map(item -> {
                   var product = new Product(
                           item.productId(),
                           products.get(item.productId()).name(),
                           new Money(products.get(item.productId()).price())
                   );

                   return OrderItem.create(
                           orderId,
                           product,
                           item.quantity()
                   );
                })
                .toList();

        var order = Order.initializeOrder(
                orderId,
                new CustomerId(command.customerId()),
                new BusinessId(command.businessId()),
                address,
                items
        );

        var saved = orderRepository.save(order);

        return new CreateOrderResult(
                saved.orderId().id(),
                saved.trackingId().id(),
                saved.status(),
                saved.price().amount()
        );
    }
}
