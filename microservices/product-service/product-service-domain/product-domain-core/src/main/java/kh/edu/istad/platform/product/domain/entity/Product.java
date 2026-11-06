package kh.edu.istad.platform.product.domain.entity;

import kh.edu.istad.platform.product.domain.exception.ProductDomainException;
import kh.edu.istad.platform.product.domain.valueobject.ProductId;

/**
 * Pure domain entity for a Product.
 * No JPA / framework annotations — framework concerns live in the persistence layer.
 */
public record Product(
        ProductId productId,
        String name,
        double price,
        int quantity
) {

    public Product {
        if (productId == null) {
            throw new ProductDomainException("Product ID is required");
        }
        if (name == null || name.isBlank()) {
            throw new ProductDomainException("Product name is required");
        }
        if (price < 0) {
            throw new ProductDomainException("Product price must not be negative");
        }
        if (quantity < 0) {
            throw new ProductDomainException("Product quantity must not be negative");
        }

        name = name.strip();
    }

    /**
     * Factory method: creates a new Product with a generated ID.
     */
    public static Product create(String name, double price, int quantity) {
        return new Product(ProductId.generate(), name, price, quantity);
    }

    /**
     * Factory method: reconstitutes a Product from persistence with an existing ID.
     */
    public static Product reconstitute(String id, String name, double price, int quantity) {
        return new Product(ProductId.of(id), name, price, quantity);
    }
}
