package kh.edu.istad.platform.order.persistence.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class OrderItemEmbeddable {
    private Integer itemId;

    private String productName;

    private int quantity;

    private BigDecimal price;

    private BigDecimal subTotal;
}
