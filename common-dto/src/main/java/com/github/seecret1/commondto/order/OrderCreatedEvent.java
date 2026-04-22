package com.github.seecret1.commondto.order;

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
