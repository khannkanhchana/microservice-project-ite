
package kh.edu.istad.platform.order.client.adapter.grpc;

import build.buf.gen.product.ProductGrpcServiceGrpc;
import build.buf.gen.product.ProductRequest;
import build.buf.gen.product.ProductResponse;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import kh.edu.istad.platform.order.application.dto.QueryProductResult;
import kh.edu.istad.platform.order.application.port.out.client.ProductServiceClient;
import kh.edu.istad.platform.order.client.exception.DownStreamServiceException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class ProductGrpcClientAdapter implements ProductServiceClient {

    private static final String PRODUCT_SERVICE = "product-service";

    @GrpcClient("product-service")
    private ProductGrpcServiceGrpc.ProductGrpcServiceBlockingStub productBlockingStub;

    @Value("${order.clients.product-service.timeout:3s}")
    private Duration callTimeout;

    @Override
    public Mono<QueryProductResult> getProductById(String productId) {

        return Mono.fromCallable(() -> {
                    ProductRequest request = ProductRequest.newBuilder()
                            .setProductId(productId)
                            .build();

                    ProductResponse response =
                            productBlockingStub
                                    .withDeadlineAfter(callTimeout.toNanos(), TimeUnit.NANOSECONDS)
                                    .getProductById(request);

                    return new QueryProductResult(
                            response.getProductId(),
                            response.getName(),
                            response.getPrice(),
                            response.getQuantity()
                    );
                })
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorMap(
                        StatusRuntimeException.class,
                        this::translateGrpcException
                );
    }

    private DownStreamServiceException translateGrpcException(StatusRuntimeException ex) {
        Status.Code code = ex.getStatus().getCode();
        String message = switch (code) {
            case NOT_FOUND -> "Product was not found";
            case INVALID_ARGUMENT -> "Product service rejected the request";
            case UNAVAILABLE -> "Product service is unavailable";
            case DEADLINE_EXCEEDED -> "Product service request timed out";
            case INTERNAL -> "Product service failed internally";
            default -> "Product service request failed";
        };

        log.warn("Downstream gRPC call failed: service={}, operation={}, statusCode={}",
                PRODUCT_SERVICE, "getProductById", code, ex);
        return new DownStreamServiceException(PRODUCT_SERVICE, message, ex);
    }
}
