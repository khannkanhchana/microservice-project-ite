package kh.edu.istad.platform.business.restapi.controller;

import jakarta.validation.Valid;
import kh.edu.istad.platform.business.application.dto.RegisterBusinessResult;
import kh.edu.istad.platform.business.application.port.out.BusinessOperations;
import kh.edu.istad.platform.business.application.usecase.RegisterBusinessUseCase;
import kh.edu.istad.platform.business.restapi.dto.RegisterBusinessRequest;
import kh.edu.istad.platform.business.restapi.mapper.BusinessWebMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.UUID;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;

@RestController
@RequestMapping("/api/businesses")
@RequiredArgsConstructor
public class BusinessController {

    private final RegisterBusinessUseCase registerBusinessUseCase;
    private final BusinessWebMapper businessWebMapper;
    private final BusinessOperations operations;

    @GetMapping("/{businessId}")
    public Mono<BusinessOperations.View> get(@PathVariable("businessId") UUID id) {
        return operations.get(id);
    }

    public record BusinessStatusRequest(@NotNull Boolean active) {}

    @PatchMapping("/{businessId}/status")
    public Mono<BusinessOperations.View> status(@PathVariable("businessId") UUID id,
            @Valid @RequestBody BusinessStatusRequest request) {
        return operations.setActive(id, request.active());
    }

    public record ProductRequest(
            @NotBlank @Size(max=255) String name,
            @NotNull @DecimalMin("0.01") @Digits(integer=17, fraction=2) BigDecimal price,
            @Min(0) int quantity, boolean available) {}

    @PutMapping("/{businessId}/products/{productId}")
    public Mono<BusinessOperations.View> product(
            @PathVariable("businessId") UUID businessId, @PathVariable("productId") UUID productId,
            @Valid @RequestBody ProductRequest request) {
        return operations.putProduct(businessId, productId, new BusinessOperations.CatalogProduct(
                productId, request.name(), request.price(), request.quantity(), request.available()));
    }

    @PostMapping("/{businessId}/orders/{orderId}/approval")
    public Mono<BusinessOperations.Approval> approve(
            @PathVariable("businessId") UUID businessId, @PathVariable("orderId") UUID orderId) {
        return operations.approve(businessId, orderId);
    }



    @GetMapping
    public Flux<BusinessOperations.View> getAll(){
        return operations.list();
    }


    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<RegisterBusinessResult> register(@Valid @RequestBody RegisterBusinessRequest request) {
        return registerBusinessUseCase.execute(businessWebMapper.toCommand(request));
    }
}
