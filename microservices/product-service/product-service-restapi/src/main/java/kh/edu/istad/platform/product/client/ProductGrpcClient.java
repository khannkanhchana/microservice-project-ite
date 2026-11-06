package kh.edu.istad.platform.product.client;


import build.buf.gen.product.CreateProductRequest;
import build.buf.gen.product.ProductGrpcServiceGrpc;
import build.buf.gen.product.ProductRequest;
import build.buf.gen.product.ProductResponse;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Service
public class ProductGrpcClient {

    @GrpcClient("product-service")
    private ProductGrpcServiceGrpc.ProductGrpcServiceBlockingStub productBlockingStub;

    public ProductResponse getProductById(String productID) {
        ProductRequest request = ProductRequest.newBuilder()
                .setProductId(productID)
                .build();

        return productBlockingStub.getProductById(request);
    }

    public ProductResponse createProduct(String name, double price, int quantity) {
        CreateProductRequest request = CreateProductRequest.newBuilder()
                .setName(name)
                .setPrice(price)
                .setQuantity(quantity)
                .build();

        return productBlockingStub.createProduct(request);
    }
}
