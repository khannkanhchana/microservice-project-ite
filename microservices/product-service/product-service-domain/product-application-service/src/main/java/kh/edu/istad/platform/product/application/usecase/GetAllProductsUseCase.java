package kh.edu.istad.platform.product.application.usecase;

import kh.edu.istad.platform.product.application.dto.ProductResult;
import kh.edu.istad.platform.product.application.port.out.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Application-layer use case: returns all products.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GetAllProductsUseCase {

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<ProductResult> execute() {
        return productRepository.findAll()
                .stream()
                .map(p -> new ProductResult(p.productId().id(), p.name(), p.price(), p.quantity()))
                .toList();
    }
}
