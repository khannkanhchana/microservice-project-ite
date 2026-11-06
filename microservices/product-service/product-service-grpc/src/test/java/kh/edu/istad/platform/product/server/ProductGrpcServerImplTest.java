package kh.edu.istad.platform.product.server;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import kh.edu.istad.platform.product.application.dto.CreateProductCommand;
import kh.edu.istad.platform.product.application.dto.ProductResult;
import kh.edu.istad.platform.product.application.usecase.CreateProductUseCase;
import kh.edu.istad.platform.product.application.usecase.GetProductByIdUseCase;
import kh.edu.istad.platform.product.domain.exception.ProductDomainException;
import kh.edu.istad.product.grpc.CreateProductRequest;
import kh.edu.istad.product.grpc.ProductRequest;
import kh.edu.istad.product.grpc.ProductResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.TransientDataAccessResourceException;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductGrpcServerImplTest {

    private GetProductByIdUseCase getProductByIdUseCase;
    private CreateProductUseCase createProductUseCase;
    private ProductGrpcServerImpl server;

    @BeforeEach
    void setUp() {
        getProductByIdUseCase = mock(GetProductByIdUseCase.class);
        createProductUseCase = mock(CreateProductUseCase.class);
        server = new ProductGrpcServerImpl(getProductByIdUseCase, createProductUseCase);
    }

    @Test
    void getProductByIdMapsApplicationResultToResponse() {
        when(getProductByIdUseCase.execute("p-1"))
                .thenReturn(new ProductResult("p-1", "Desk", 89.5, 4));
        RecordingObserver observer = new RecordingObserver();

        server.getProductById(ProductRequest.newBuilder().setProductId("p-1").build(), observer);

        assertEquals(new ProductResponseSnapshot("p-1", "Desk", 89.5, 4),
                ProductResponseSnapshot.from(observer.value));
        assertTrue(observer.completed);
        assertNull(observer.error);
    }

    @Test
    void createProductDelegatesCommandAndMapsResult() {
        when(createProductUseCase.execute(any(CreateProductCommand.class)))
                .thenReturn(new ProductResult("p-2", "Chair", 35.0, 7));
        RecordingObserver observer = new RecordingObserver();

        server.createProduct(CreateProductRequest.newBuilder()
                .setName("Chair").setPrice(35.0).setQuantity(7).build(), observer);

        verify(createProductUseCase).execute(new CreateProductCommand("Chair", 35.0, 7));
        assertEquals(new ProductResponseSnapshot("p-2", "Chair", 35.0, 7),
                ProductResponseSnapshot.from(observer.value));
        assertTrue(observer.completed);
        assertNull(observer.error);
    }

    @Test
    void mapsDomainValidationToInvalidArgument() {
        when(getProductByIdUseCase.execute(" ")).thenThrow(new ProductDomainException("Product ID is required"));

        assertStatus(Status.Code.INVALID_ARGUMENT, observer ->
                server.getProductById(ProductRequest.newBuilder().setProductId(" ").build(), observer));
    }

    @Test
    void mapsMissingProductToNotFound() {
        when(getProductByIdUseCase.execute("missing")).thenThrow(new NoSuchElementException("missing"));

        assertStatus(Status.Code.NOT_FOUND, observer ->
                server.getProductById(ProductRequest.newBuilder().setProductId("missing").build(), observer));
    }

    @Test
    void mapsDuplicateAndTransientPersistenceErrors() {
        when(createProductUseCase.execute(any(CreateProductCommand.class)))
                .thenThrow(new DuplicateKeyException("duplicate"));
        assertStatus(Status.Code.ALREADY_EXISTS, observer ->
                server.createProduct(CreateProductRequest.getDefaultInstance(), observer));

        when(createProductUseCase.execute(any(CreateProductCommand.class)))
                .thenThrow(new TransientDataAccessResourceException("database unavailable"));
        assertStatus(Status.Code.UNAVAILABLE, observer ->
                server.createProduct(CreateProductRequest.getDefaultInstance(), observer));
    }

    @Test
    void mapsUnexpectedErrorsToInternal() {
        when(getProductByIdUseCase.execute("p-1")).thenThrow(new IllegalStateException("unexpected"));

        assertStatus(Status.Code.INTERNAL, observer ->
                server.getProductById(ProductRequest.newBuilder().setProductId("p-1").build(), observer));
    }

    private static void assertStatus(Status.Code expected, RpcCall call) {
        RecordingObserver observer = new RecordingObserver();
        call.invoke(observer);
        assertEquals(expected, Status.fromThrowable(observer.error).getCode());
        assertNull(observer.value);
        assertTrue(observer.error instanceof RuntimeException);
    }

    @FunctionalInterface
    private interface RpcCall {
        void invoke(RecordingObserver observer);
    }

    private static final class RecordingObserver implements StreamObserver<ProductResponse> {
        private ProductResponse value;
        private Throwable error;
        private boolean completed;

        @Override
        public void onNext(ProductResponse value) {
            this.value = value;
        }

        @Override
        public void onError(Throwable error) {
            this.error = error;
        }

        @Override
        public void onCompleted() {
            completed = true;
        }
    }

    private record ProductResponseSnapshot(String id, String name, double price, int quantity) {
        private static ProductResponseSnapshot from(ProductResponse response) {
            return new ProductResponseSnapshot(response.getProductId(), response.getName(),
                    response.getPrice(), response.getQuantity());
        }
    }
}
