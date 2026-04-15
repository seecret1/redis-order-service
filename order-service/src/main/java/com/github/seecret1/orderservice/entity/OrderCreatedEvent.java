package com.github.seecret1.orderservice.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Builder
@Table(name = "Order_created_events")
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderCreatedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID orderId;

    String userId;

    String productCode;

    int quantity;

    BigDecimal totalPrice;

    Instant timestamp;

    @PrePersist
    public void setTimestamp() {
        timestamp = Instant.now();
    }
}
