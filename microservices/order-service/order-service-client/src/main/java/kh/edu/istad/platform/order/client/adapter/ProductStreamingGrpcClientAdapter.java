//package kh.edu.istad.platform.order.client.adapter;
//
//
//import build.buf.gen.product.ProductGrpcServiceGrpc;
//import io.grpc.StatusRuntimeException;
//import io.grpc.stub.ClientCallStreamObserver;
//import io.grpc.stub.ClientResponseObserver;
//import kh.edu.istad.platform.order.application.dto.QueryProductResult;
//import kh.edu.istad.platform.order.application.port.out.client.ProductStreamClient;
//import kh.edu.istad.platform.order.domain.port.out.client.ProductStreamClient;
//import kh.edu.istad.product.grpc.ListProductsRequest;
//import kh.edu.istad.product.grpc.ProductGrpcServiceGrpc;
//import kh.edu.istad.product.grpc.ProductResponse;
//import net.devh.boot.grpc.client.inject.GrpcClient;
//import org.springframework.http.HttpStatus;
//import org.springframework.stereotype.Component;
//import org.springframework.web.server.ResponseStatusException;
//import reactor.core.publisher.Flux;
//
//import java.util.concurrent.TimeUnit;
//
//@Component
//public class ProductStreamingGrpcClientAdapter implements ProductStreamClient {
//
//    @GrpcClient("product-service")
//    private ProductGrpcServiceGrpc.ProductGrpcServiceStub productAsyncStub;
//
//    @Override
//    public Flux<QueryProductResult> listProducts() {
//        return Flux.<QueryProductResult>create(sink ->
//                        productAsyncStub.withDeadlineAfter(30, TimeUnit.SECONDS).listProducts(
//                                ListProductsRequest.getDefaultInstance(),
//                                new ClientResponseObserver<ListProductsRequest, ProductResponse>() {
//                                    @Override
//                                    public void beforeStart(ClientCallStreamObserver<ListProductsRequest> call) {
//                                        // Stop the remote call when the HTTP subscriber disconnects.
//                                        sink.onCancel(() -> call.cancel("HTTP subscriber cancelled", null));
//                                    }
//
//                                    @Override
//                                    public void onNext(ProductResponse response) {
//                                        sink.next(new QueryProductResult(response.getProductId(),
//                                                response.getName(), response.getPrice(), response.getQuantity()));
//                                    }
//
//                                    @Override
//                                    public void onError(Throwable error) {
//                                        sink.error(error);
//                                    }
//
//                                    @Override
//                                    public void onCompleted() {
//                                        sink.complete();
//                                    }
//                                }))
//                .onErrorMap(StatusRuntimeException.class, error -> {
//                    HttpStatus status = switch (error.getStatus().getCode()) {
//                        case UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
//                        case DEADLINE_EXCEEDED -> HttpStatus.GATEWAY_TIMEOUT;
//                        default -> HttpStatus.INTERNAL_SERVER_ERROR;
//                    };
//                    return new ResponseStatusException(status, "Product stream failed", error);
//                });
//    }
//}
//
