package kh.edu.istad.platform.order.restapi.controller;

import jakarta.validation.Valid;
import kh.edu.istad.platform.order.application.dto.CreateOrderResult;
import kh.edu.istad.platform.order.application.usecase.CreateOrderUseCase;
import kh.edu.istad.platform.order.application.usecase.GetAllOrderUseCase;
import kh.edu.istad.platform.order.application.usecase.GetOrderByIdUseCase;
import kh.edu.istad.platform.order.restapi.dto.CreateOrderRequest;
import kh.edu.istad.platform.order.restapi.dto.OrderResponse;
import kh.edu.istad.platform.order.restapi.mapper.OrderWebMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import reactor.core.publisher.Mono;


@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private final GetOrderByIdUseCase getOrderByIdUseCase;
   private final CreateOrderUseCase createOrderUseCase;
   private final OrderWebMapper orderWebMapper;
   private final GetAllOrderUseCase getAllOrderUseCase;

   @GetMapping
   public Mono<Page<OrderResponse>> getAllOrders(@PageableDefault(size = 10,
   sort = "id",
   direction = Sort.Direction.DESC) Pageable pageable){
       return getAllOrderUseCase.execute(pageable)
               .map(page -> page.map(orderWebMapper::orderToOrderResponse));
   }
   @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<CreateOrderResult> create(
            @Valid @RequestBody CreateOrderRequest createOrderRequest
   ){
       return createOrderUseCase.execute(orderWebMapper.toCommand(createOrderRequest));
   }

    @GetMapping("/{orderId}")
    public Mono<OrderResponse> getOrderById(
            @PathVariable("orderId") UUID orderId
    ) {
        return getOrderByIdUseCase.execute(orderId)
                .map(orderWebMapper::orderToOrderResponse);
    }


}
