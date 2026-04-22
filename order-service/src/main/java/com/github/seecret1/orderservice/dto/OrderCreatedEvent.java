package com.github.seecret1.orderservice.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderCreatedEvent (

    UUID orderId,

    String userId,

    String productCode,

    int quantity,

    BigDecimal totalPrice,

    Instant timestamp

) { }
