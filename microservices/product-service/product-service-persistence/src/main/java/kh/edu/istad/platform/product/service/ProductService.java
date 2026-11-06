package kh.edu.istad.platform.product.service;

import org.springframework.transaction.annotation.Transactional;
import kh.edu.istad.platform.product.entity.Product;
import kh.edu.istad.platform.product.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public Product findByID(String id){
        return productRepository.findById(id).orElse(null);
    }

    @Transactional
    public Product create(String name, double price, int quantity){

        String id = UUID.randomUUID().toString();

        Product product = Product.builder()
                .id(id)
                .name(name)
                .price(price)
                .quantity(quantity)
                .build();
        return productRepository.save(product);
    }
}