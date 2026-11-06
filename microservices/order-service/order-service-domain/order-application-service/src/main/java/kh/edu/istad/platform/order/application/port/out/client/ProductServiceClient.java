package kh.edu.istad.platform.order.application.port.out.client;

import kh.edu.istad.platform.order.application.dto.QueryProductResult;

import reactor.core.publisher.Mono;

public interface ProductServiceClient {
    Mono<QueryProductResult> getProductById(String productId);
}