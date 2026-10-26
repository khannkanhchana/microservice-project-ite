package kh.edu.istad.platform.payment.persistence.repository;



import kh.edu.istad.platform.payment.domain.entity.CreditEntry;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface CreditEntryRepository {

    Mono<CreditEntry> save(CreditEntry creditEntry);

    Mono<CreditEntry> findByCustomerId(UUID customerId);
}