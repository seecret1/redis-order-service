package com.github.seecret1.orderservice.api;

import com.github.seecret1.commondto.order.CreateOrderRequest;
import com.github.seecret1.commondto.order.OrderCreatedEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface OrderMapper {

    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "productCode", source = "productCode")
    @Mapping(target = "quantity", source = "quantity")
    @Mapping(target = "totalPrice", source = "totalPrice")
    Order toEntity(CreateOrderRequest request);

    @Mapping(target = "orderId", source = "id")
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "productCode", source = "productCode")
    @Mapping(target = "quantity", source = "quantity")
    @Mapping(target = "totalPrice", source = "totalPrice")
    OrderCreatedEvent toDto(Order order);

    List<OrderCreatedEvent> toDto(List<Order> orders);
}
