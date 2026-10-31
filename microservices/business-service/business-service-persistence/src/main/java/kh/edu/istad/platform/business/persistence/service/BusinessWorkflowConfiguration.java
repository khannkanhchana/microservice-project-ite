package kh.edu.istad.platform.business.persistence.service;

import java.time.Duration;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory;
import org.springframework.data.mongodb.ReactiveMongoTransactionManager;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import reactor.netty.http.client.HttpClient;

@Configuration
public class BusinessWorkflowConfiguration {
    @Bean
    TransactionalOperator businessTransactions(ReactiveMongoDatabaseFactory factory) {
        return TransactionalOperator.create(new ReactiveMongoTransactionManager(factory));
    }

    @Bean
    @LoadBalanced
    WebClient.Builder businessDiscoveryClient() {
        var http = HttpClient.create().option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
                .responseTimeout(Duration.ofSeconds(8));
        return WebClient.builder().clientConnector(new ReactorClientHttpConnector(http));
    }

    @Bean
    SmartInitializingSingleton approvalCollection(ReactiveMongoTemplate template) {
        return () -> template.collectionExists(BusinessOperationsService.ApprovalRecord.class)
                .flatMap(exists -> exists ? reactor.core.publisher.Mono.just(true)
                        : template.createCollection(BusinessOperationsService.ApprovalRecord.class).map(collection -> true))
                .block(Duration.ofSeconds(15));
    }
}
