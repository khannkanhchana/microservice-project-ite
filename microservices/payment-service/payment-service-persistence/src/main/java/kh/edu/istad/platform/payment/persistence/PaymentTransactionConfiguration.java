package kh.edu.istad.platform.payment.persistence;

import java.time.Duration;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.*;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.ReactiveMongoTransactionManager;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.transaction.reactive.TransactionalOperator;
import kh.edu.istad.platform.payment.persistence.document.*;

@Configuration
public class PaymentTransactionConfiguration {
    @Bean
    TransactionalOperator paymentTransactions(ReactiveMongoDatabaseFactory factory) {
        return TransactionalOperator.create(new ReactiveMongoTransactionManager(factory));
    }

    @Bean
    SmartInitializingSingleton paymentUniqueIndexes(ReactiveMongoTemplate template) {
        return () -> template.indexOps(PaymentDocument.class).createIndex(new Index().on("orderId", Sort.Direction.ASC).unique())
                .then(template.indexOps(CreditEntryDocument.class).createIndex(new Index().on("customerId", Sort.Direction.ASC).unique()))
                .block(Duration.ofSeconds(15));
    }
}
