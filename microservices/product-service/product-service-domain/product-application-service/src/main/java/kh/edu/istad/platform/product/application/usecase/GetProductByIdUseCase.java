package kh.edu.istad.platform.product.application.usecase;

import kh.edu.istad.platform.product.application.dto.ProductResult;
import kh.edu.istad.platform.product.application.port.out.ProductRepository;
import kh.edu.istad.platform.product.domain.exception.ProductDomainException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

/**
 * Application-layer use case: retrieves a single product by its ID.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GetProductByIdUseCase {

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public ProductResult execute(String productId) {

        if (productId == null || productId.isBlank()) {
            throw new ProductDomainException("Product ID is required");
        }

        return productRepository
                .findById(productId)
                .map(p -> new ProductResult(p.productId().id(), p.name(), p.price(), p.quantity()))
                .orElseThrow(() -> {
                    log.warn("Product not found: id={}", productId);
                    return new NoSuchElementException("Product not found with ID: " + productId);
                });
    }
}
