package kh.edu.istad.platform.customer.persistence.entity;

import kh.edu.istad.platform.customer.domain.valueobject.CustomerStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Table("customers")
public class CustomerEntity {
    @Id
    private UUID customerId;
    private String username;
    private String familyName;
    private String givenName;
    private String email;
    private String phoneNumber;
    private CustomerStatus status;
    private String createdBy;
    private Instant createdAt;
    private String modifiedBy;
    private String modifiedAt;
}