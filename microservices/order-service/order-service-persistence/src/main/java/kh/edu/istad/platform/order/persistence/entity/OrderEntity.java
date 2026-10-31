
package kh.edu.istad.platform.order.persistence.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Table("orders")
public class OrderEntity {

    @Id
    private UUID id;

    private UUID customerId;
    private UUID businessId;
    @Column("failure_messages")
    private String failureMessage;
    private UUID trackingId;
    @Column("status")
    private String orderStatus;
    private BigDecimal price;

    private UUID addressId;
    private String street;
    private String postalCode;
    private String city;

    // R2DBC does not automatically persist child collections.
    @Transient
    private List<OrderItemEmbeddable> items = List.of();
}
