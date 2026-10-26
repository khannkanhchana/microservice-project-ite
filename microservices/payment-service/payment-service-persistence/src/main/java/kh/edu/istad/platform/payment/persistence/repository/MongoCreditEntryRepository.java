package kh.edu.istad.platform.payment.persistence.repository;


import kh.edu.istad.platform.payment.domain.entity.CreditEntry;
import kh.edu.istad.platform.payment.domain.valueobject.CreditEntryId;
import kh.edu.istad.platform.payment.domain.valueobject.CustomerId;
import kh.edu.istad.platform.payment.domain.valueobject.Money;
import kh.edu.istad.platform.payment.persistence.document.CreditEntryDocument;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public class MongoCreditEntryRepository
        implements CreditEntryRepository {

    private final ReactiveMongoTemplate mongoTemplate;

    public MongoCreditEntryRepository(
            ReactiveMongoTemplate mongoTemplate
    ) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Mono<CreditEntry> save(CreditEntry creditEntry) {
        CreditEntryDocument document = toDocument(creditEntry);

        return mongoTemplate.save(document)
                .map(this::toDomain);
    }

    @Override
    public Mono<CreditEntry> findByCustomerId(UUID customerId) {
        if (customerId == null) {
            return Mono.error(
                    new IllegalArgumentException("Customer ID is required")
            );
        }

        Query query = Query.query(
                Criteria.where("customerId").is(customerId)
        );

        return mongoTemplate.findOne(query, CreditEntryDocument.class)
                .map(this::toDomain);
    }

    private CreditEntryDocument toDocument(CreditEntry creditEntry) {
        CreditEntryDocument document = new CreditEntryDocument();

        document.setId(creditEntry.id().value().toString());
        document.setCustomerId(creditEntry.customerId().value());
        document.setTotalCreditAmount(
                creditEntry.totalCreditAmount().amount()
        );

        return document;
    }

    private CreditEntry toDomain(CreditEntryDocument document) {
        return new CreditEntry(
                new CreditEntryId(UUID.fromString(document.getId())),
                new CustomerId(document.getCustomerId()),
                new Money(document.getTotalCreditAmount())
        );
    }
}