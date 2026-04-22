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

    @Column(name = "user_id")
    String userId;

    @Column(name = "product_code")
    String productCode;

    @Column(name = "quantity")
    int quantity;

    @Column(name = "total_price")
    BigDecimal totalPrice;

    @CreationTimestamp
    @Column(name = "timestamp")
    Instant timestamp;
}
