package kh.edu.istad.platform.payment.restapi.controller;

import java.math.BigDecimal;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import kh.edu.istad.platform.payment.persistence.PaymentPersistenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/** Course credit-account provisioning; a production deposit needs an authenticated funding provider. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/credit-accounts")
public class CreditAccountController {
    private final PaymentPersistenceService payments;
    public record OpenAccount(@NotNull UUID customerId,
            @NotNull @DecimalMin("0.00") @Digits(integer=17, fraction=2) BigDecimal initialBalance) {}

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<PaymentPersistenceService.Account> create(@Valid @RequestBody OpenAccount request) {
        return payments.openAccount(request.customerId(), request.initialBalance());
    }

    @GetMapping("/{customerId}")
    public Mono<PaymentPersistenceService.Account> get(@PathVariable("customerId") UUID customerId) {
        return payments.account(customerId);
    }
}
