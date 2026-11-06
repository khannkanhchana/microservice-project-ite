package kh.edu.istad.platform.product.server;


import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import kh.edu.istad.platform.product.entity.Product;
import kh.edu.istad.platform.product.service.ProductService;
import kh.edu.istad.product.grpc.CreateProductRequest;
import kh.edu.istad.product.grpc.ProductGrpcServiceGrpc;
import kh.edu.istad.product.grpc.ProductRequest;
import kh.edu.istad.product.grpc.ProductResponse;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
@RequiredArgsConstructor
public class ProductGrpcServerImpl extends ProductGrpcServiceGrpc.ProductGrpcServiceImplBase{

    private final ProductService productService;

    @Override
    public void getProductById(ProductRequest request, StreamObserver<ProductResponse> responseObserver) {

        Product product = productService.findByID(request.getProductId());

        if (product == null) {
            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription("Product with ID " + request.getProductId() + " not found")
                            .asRuntimeException()
            );
            return;
        }

        ProductResponse response = ProductResponse.newBuilder()
                .setProductId(product.getId())
                .setName(product.getName())
                .setPrice(product.getPrice())
                .setQuantity(product.getQuantity())
                .build();

        responseObserver.onNext(response); //
        responseObserver.onCompleted(); // end proccess
    }

    // Create Product
    @Override
    public void createProduct(CreateProductRequest request, StreamObserver<ProductResponse> responseObserver) {

        Product product = productService.create(
                request.getName(),
                request.getPrice(),
                request.getQuantity()
        );

        ProductResponse response = ProductResponse.newBuilder()
                .setProductId(product.getId())
                .setName(product.getName())
                .setPrice(product.getPrice())
                .setQuantity(product.getQuantity())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted(); //
    }

}
