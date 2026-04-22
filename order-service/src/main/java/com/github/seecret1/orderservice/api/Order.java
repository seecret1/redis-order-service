package com.github.seecret1.orderservice.api;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Builder
@Table(name = "orders")
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    UUID id;

    @Column(name = "user_id", nullable = false)
    String userId;

    @Column(name = "product_code", nullable = false)
    String productCode;

    @Column(name = "quantity", nullable = false)
    int quantity;

    @Column(name = "total_price", nullable = false)
    BigDecimal totalPrice;

    @CreationTimestamp
    @Column(name = "timestamp", nullable = false)
    Instant timestamp;
}
