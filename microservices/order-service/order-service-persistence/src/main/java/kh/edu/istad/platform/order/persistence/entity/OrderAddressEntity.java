package kh.edu.istad.platform.order.persistence.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;


@Getter
@Setter
@NoArgsConstructor
@Table("order_addresses")
public class OrderAddressEntity {
    private UUID id;
    private String city;
    private String postalCode;
    private String street;
}
