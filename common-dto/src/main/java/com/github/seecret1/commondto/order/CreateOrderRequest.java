package com.github.seecret1.commondto.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CreateOrderRequest(

        @NotBlank(message = "user id must be set!")
        String userId,

        @NotBlank(message = "product code must be set!")
        String productCode,

        @Positive(message = "quantity must be positive value!")
        int quantity,

        @NotNull(message = "total price must be set!")
        @Positive(message = "total price must be positive value!")
        BigDecimal totalPrice
) { }
