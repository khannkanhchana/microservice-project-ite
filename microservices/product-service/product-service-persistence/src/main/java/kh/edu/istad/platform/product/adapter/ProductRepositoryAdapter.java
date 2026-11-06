package kh.edu.istad.platform.product.adapter;

import kh.edu.istad.platform.product.application.port.out.ProductRepository;
import kh.edu.istad.platform.product.entity.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ProductRepositoryAdapter implements ProductRepository {

    private final kh.edu.istad.platform.product.repository.ProductRepository productJpaRepository;

    @Override
    public kh.edu.istad.platform.product.domain.entity.Product save(
            kh.edu.istad.platform.product.domain.entity.Product product
    ) {
        Product entity = new Product(
                product.productId().id(),
                product.name(),
                product.price(),
                product.quantity()
        );
        return toDomain(productJpaRepository.save(entity));
    }

    @Override
    public Optional<kh.edu.istad.platform.product.domain.entity.Product> findById(String id) {
        return productJpaRepository.findById(id).map(ProductRepositoryAdapter::toDomain);
    }

    @Override
    public List<kh.edu.istad.platform.product.domain.entity.Product> findAll() {
        return productJpaRepository.findAll().stream()
                .map(ProductRepositoryAdapter::toDomain)
                .toList();
    }

    @Override
    public boolean existsById(String id) {
        return productJpaRepository.existsById(id);
    }

    private static kh.edu.istad.platform.product.domain.entity.Product toDomain(Product product) {
        return kh.edu.istad.platform.product.domain.entity.Product.reconstitute(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getQuantity()
        );
    }
}
