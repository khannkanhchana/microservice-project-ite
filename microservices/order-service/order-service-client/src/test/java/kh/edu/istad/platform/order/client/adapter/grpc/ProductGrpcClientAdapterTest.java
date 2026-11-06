package kh.edu.istad.platform.order.client.adapter.grpc;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import kh.edu.istad.platform.order.client.exception.DownStreamServiceException;
import kh.edu.istad.product.grpc.ProductGrpcServiceGrpc;
import kh.edu.istad.product.grpc.ProductRequest;
import kh.edu.istad.product.grpc.ProductResponse;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductGrpcClientAdapterTest {

    @Test
    void usesBlockingStubDeadlineAndMapsProductResponse() {
        ProductGrpcServiceGrpc.ProductGrpcServiceBlockingStub stub = mock(ProductGrpcServiceGrpc.ProductGrpcServiceBlockingStub.class);
        when(stub.withDeadlineAfter(anyLong(), eq(TimeUnit.NANOSECONDS))).thenReturn(stub);
        when(stub.getProductById(any(ProductRequest.class))).thenReturn(ProductResponse.newBuilder()
                .setProductId("product-1")
                .setName("Desk")
                .setPrice(89.5)
                .setQuantity(4)
                .build());
        ProductGrpcClientAdapter adapter = adapterWith(stub);

        var result = adapter.getProductById("product-1").block();

        assertEquals("product-1", result.id());
        assertEquals("Desk", result.name());
        assertEquals(89.5, result.price());
        assertEquals(4, result.quantity());
    }

    @Test
    void mapsNotFoundToDownstreamServiceException() {
        assertMapped(Status.NOT_FOUND, "Product was not found");
    }

    @Test
    void mapsInvalidArgumentToDownstreamServiceException() {
        assertMapped(Status.INVALID_ARGUMENT, "Product service rejected the request");
    }

    @Test
    void mapsUnavailableToDownstreamServiceException() {
        assertMapped(Status.UNAVAILABLE, "Product service is unavailable");
    }

    @Test
    void mapsDeadlineExceededToDownstreamServiceException() {
        assertMapped(Status.DEADLINE_EXCEEDED, "Product service request timed out");
    }

    @Test
    void mapsInternalToDownstreamServiceException() {
        assertMapped(Status.INTERNAL, "Product service failed internally");
    }

    private static ProductGrpcClientAdapter adapterWith(
            ProductGrpcServiceGrpc.ProductGrpcServiceBlockingStub stub
    ) {
        ProductGrpcClientAdapter adapter = new ProductGrpcClientAdapter();
        ReflectionTestUtils.setField(adapter, "productBlockingStub", stub);
        ReflectionTestUtils.setField(adapter, "callTimeout", Duration.ofSeconds(3));
        return adapter;
    }

    private static void assertMapped(Status status, String expectedMessage) {
        ProductGrpcServiceGrpc.ProductGrpcServiceBlockingStub stub = mock(ProductGrpcServiceGrpc.ProductGrpcServiceBlockingStub.class);
        when(stub.withDeadlineAfter(anyLong(), eq(TimeUnit.NANOSECONDS))).thenReturn(stub);
        StatusRuntimeException grpcError = status.asRuntimeException();
        when(stub.getProductById(any(ProductRequest.class))).thenThrow(grpcError);
        ProductGrpcClientAdapter adapter = adapterWith(stub);

        DownStreamServiceException error = assertThrows(DownStreamServiceException.class,
                () -> adapter.getProductById("product-1").block());

        assertEquals(expectedMessage, error.getMessage());
        assertEquals("product-service", error.getService());
        assertSame(grpcError, error.getCause());
    }
}
