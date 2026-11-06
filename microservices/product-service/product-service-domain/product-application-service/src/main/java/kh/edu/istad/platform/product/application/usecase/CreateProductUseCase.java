package kh.edu.istad.platform.product.application.usecase;

import kh.edu.istad.platform.product.application.dto.CreateProductCommand;
import kh.edu.istad.platform.product.application.dto.ProductResult;
import kh.edu.istad.platform.product.application.port.out.ProductRepository;
import kh.edu.istad.platform.product.domain.entity.Product;
import kh.edu.istad.platform.product.domain.exception.ProductDomainException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application-layer use case: creates a new product.
 * Delegates domain construction to {@link Product#create} and persistence to {@link ProductRepository}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreateProductUseCase {

    private final ProductRepository productRepository;

    @Transactional
    public ProductResult execute(CreateProductCommand command) {

        if (command == null) {
            throw new ProductDomainException("CreateProductCommand must not be null");
        }
        if (command.name() == null || command.name().isBlank()) {
            throw new ProductDomainException("Product name is required");
        }
        if (command.price() < 0) {
            throw new ProductDomainException("Product price must not be negative");
        }
        if (command.quantity() < 0) {
            throw new ProductDomainException("Product quantity must not be negative");
        }

        Product product = Product.create(
                command.name(),
                command.price(),
                command.quantity()
        );

        Product saved = productRepository.save(product);

        log.info("Product created: id={}, name={}", saved.productId().id(), saved.name());

        return toResult(saved);
    }

    private static ProductResult toResult(Product p) {
        return new ProductResult(p.productId().id(), p.name(), p.price(), p.quantity());
    }
}
