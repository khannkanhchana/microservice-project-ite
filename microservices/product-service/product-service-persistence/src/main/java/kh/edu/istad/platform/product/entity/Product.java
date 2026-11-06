package kh.edu.istad.platform.product.entity;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Getter
@Setter
@Entity
@Table(name = "products")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    private String id;

    private String name;
    private double price;
    private int quantity;
}