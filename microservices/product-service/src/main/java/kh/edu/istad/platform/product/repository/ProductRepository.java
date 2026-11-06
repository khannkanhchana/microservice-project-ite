package kh.edu.istad.platform.product.repository;


import kh.edu.istad.platform.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, String> {

}
