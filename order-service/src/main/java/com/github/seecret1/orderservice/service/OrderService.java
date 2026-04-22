package com.github.seecret1.orderservice.service;

import com.github.seecret1.commondto.order.CreateOrderRequest;
import com.github.seecret1.commondto.order.OrderCreatedEvent;
import com.github.seecret1.orderservice.api.Order;
import com.github.seecret1.orderservice.api.OrderMapper;
import com.github.seecret1.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    private final OrderMapper orderMapper;

    private final OrderProducer eventPublisher;

    @Transactional
    public OrderCreatedEvent createOrder(CreateOrderRequest request) {
        Order order = orderMapper.toEntity(request);
        order.setId(UUID.randomUUID());
        order.setTimestamp(Instant.now());

        Order savedOrder = orderRepository.save(order);
        log.info("Order saved with id: {}", savedOrder.getId());

        OrderCreatedEvent event = orderMapper.toDto(savedOrder);
        eventPublisher.publishOrderCreated(event);

        return event;
    }
}
