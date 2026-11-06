package kh.edu.istad.platform.product.server;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import kh.edu.istad.platform.product.application.dto.CreateProductCommand;
import kh.edu.istad.platform.product.application.dto.ProductResult;
import kh.edu.istad.platform.product.application.usecase.CreateProductUseCase;
import kh.edu.istad.platform.product.application.usecase.GetProductByIdUseCase;
import kh.edu.istad.platform.product.domain.exception.ProductDomainException;
import build.buf.gen.product.CreateProductRequest;
import build.buf.gen.product.ProductGrpcServiceGrpc;
import build.buf.gen.product.ProductRequest;
import build.buf.gen.product.ProductResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.TransientDataAccessException;

import java.sql.SQLTransientException;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeoutException;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class ProductGrpcServerImpl extends ProductGrpcServiceGrpc.ProductGrpcServiceImplBase {

    private final GetProductByIdUseCase getProductByIdUseCase;
    private final CreateProductUseCase createProductUseCase;

    @Override
    public void getProductById(
            ProductRequest request,
            StreamObserver<ProductResponse> responseObserver
    ) {
        try {
            ProductResult product = getProductByIdUseCase.execute(request.getProductId());
            responseObserver.onNext(toResponse(product));
            responseObserver.onCompleted();
            log.info("Product lookup completed: productId={}", product.id());
        } catch (RuntimeException exception) {
            respondWithError("getProductById", request.getProductId(), exception, responseObserver);
        }
    }

    @Override
    public void createProduct(
            CreateProductRequest request,
            StreamObserver<ProductResponse> responseObserver
    ) {
        try {
            CreateProductCommand command = new CreateProductCommand(
                    request.getName(),
                    request.getPrice(),
                    request.getQuantity()
            );
            ProductResult product = createProductUseCase.execute(command);
            responseObserver.onNext(toResponse(product));
            responseObserver.onCompleted();
            log.info("Product created through gRPC: productId={}", product.id());
        } catch (RuntimeException exception) {
            respondWithError("createProduct", "create", exception, responseObserver);
        }
    }

    private static ProductResponse toResponse(ProductResult product) {
        return ProductResponse.newBuilder()
                .setProductId(product.id())
                .setName(product.name())
                .setPrice(product.price())
                .setQuantity(product.quantity())
                .build();
    }

    private static void respondWithError(
            String method,
            String requestKey,
            RuntimeException exception,
            StreamObserver<ProductResponse> responseObserver
    ) {
        Status status = toStatus(exception);
        if (status.getCode() == Status.Code.INTERNAL) {
            log.error("Product gRPC request failed: method={}, requestKey={}, status={}",
                    method, requestKey, status.getCode(), exception);
        } else {
            log.warn("Product gRPC request rejected: method={}, requestKey={}, status={}, reason={}",
                    method, requestKey, status.getCode(), exception.getMessage());
        }
        responseObserver.onError(status.withCause(exception).asRuntimeException());
    }

    private static Status toStatus(Throwable exception) {
        if (exception instanceof StatusRuntimeException statusException) {
            return statusException.getStatus();
        }
        if (exception instanceof NoSuchElementException) {
            return Status.NOT_FOUND.withDescription(exception.getMessage());
        }
        if (exception instanceof ProductDomainException || exception instanceof IllegalArgumentException) {
            return Status.INVALID_ARGUMENT.withDescription(exception.getMessage());
        }
        if (exception instanceof DuplicateKeyException || exception instanceof DataIntegrityViolationException) {
            return Status.ALREADY_EXISTS.withDescription("A product with the supplied identifier already exists");
        }
        if (exception instanceof TransientDataAccessException
                || exception instanceof SQLTransientException
                || exception instanceof TimeoutException) {
            return Status.UNAVAILABLE.withDescription("Product service is temporarily unavailable");
        }
        return Status.INTERNAL.withDescription("Internal product service error");
    }
}
