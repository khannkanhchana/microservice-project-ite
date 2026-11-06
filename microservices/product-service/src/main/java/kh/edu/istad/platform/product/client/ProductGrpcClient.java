package kh.edu.istad.platform.product.client;


import kh.edu.istad.product.grpc.CreateProductRequest;
import kh.edu.istad.product.grpc.ProductGrpcServiceGrpc;
import kh.edu.istad.product.grpc.ProductRequest;
import kh.edu.istad.product.grpc.ProductResponse;
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
