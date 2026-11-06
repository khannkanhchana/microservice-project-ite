package kh.edu.istad.platform.product.controller;


import kh.edu.istad.platform.product.client.ProductGrpcClient;
import kh.edu.istad.platform.product.dto.CreateProductDto;
import kh.edu.istad.platform.product.dto.ProductResponseDto;
import kh.edu.istad.product.grpc.ProductResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductGrpcClient productGrpcClient;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponseDto createProduct(@RequestBody CreateProductDto createProductDto){
        ProductResponse grpcResponse = productGrpcClient.createProduct(
                createProductDto.name(),
                createProductDto.price(),
                createProductDto.quantity()
        );

        return ProductResponseDto.fromGrpcResponse(grpcResponse);
    }

    @GetMapping("/{id}")
    public ProductResponseDto getProductById(@PathVariable String id) {
        ProductResponse grpcResponse = productGrpcClient.getProductById(id);
        return ProductResponseDto.fromGrpcResponse(grpcResponse);
    }

}
