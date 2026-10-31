package kh.edu.istad.platform.order.restapi.mapper;

import kh.edu.istad.platform.order.application.dto.CreateOrderCommand;
import kh.edu.istad.platform.order.application.dto.Item;
import kh.edu.istad.platform.order.domain.entity.Order;
import kh.edu.istad.platform.order.restapi.dto.CreateOrderRequest;
import kh.edu.istad.platform.order.restapi.dto.OrderResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface OrderWebMapper {
    @Mapping(target = "street", source = "deliveryAddress.street")
    @Mapping(target = "postalCode", source = "deliveryAddress.postalCode")
    @Mapping(target = "city", source = "deliveryAddress.city")
    CreateOrderCommand toCommand(CreateOrderRequest request);

    Item toItem(CreateOrderRequest.Item item);

    @Mapping(target = "orderId", source = "orderId.id")
    @Mapping(target = "trackingId", source = "trackingId.id")
    @Mapping(target = "totalAmount", source = "price.amount")
    @Mapping(target = "customerId", source = "customerId.id")
    @Mapping(target = "businessId", source = "businessId.id")
    OrderResponse orderToOrderResponse(Order order);

    @Mapping(target = "productId", source = "product.productId")
    @Mapping(target = "name", source = "product.name")
    @Mapping(target = "price", source = "price.amount")
    OrderResponse.Item toResponseItem(kh.edu.istad.platform.order.domain.entity.OrderItem item);
}
