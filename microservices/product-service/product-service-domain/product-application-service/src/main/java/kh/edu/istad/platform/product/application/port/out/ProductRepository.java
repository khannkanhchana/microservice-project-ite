package kh.edu.istad.platform.product.application.port.out;

import kh.edu.istad.platform.product.domain.entity.Product;

import java.util.List;
import java.util.Optional;

/**
 * Output port (secondary port) — defines what the application layer needs from persistence.
 * Implemented by the persistence adapter in product-service-persistence.
 */
public interface ProductRepository {

    /**
     * Persist a new product or update an existing one.
     *
     * @param product domain entity to save
     * @return the saved domain entity (with any persistence-generated fields populated)
     */
    Product save(Product product);

    /**
     * Find a product by its String ID.
     *
     * @param id the product UUID string
     * @return an Optional containing the domain entity, or empty if not found
     */
    Optional<Product> findById(String id);

    /**
     * Return all products.
     */
    List<Product> findAll();

    /**
     * Check whether a product with the given ID exists.
     */
    boolean existsById(String id);
}
