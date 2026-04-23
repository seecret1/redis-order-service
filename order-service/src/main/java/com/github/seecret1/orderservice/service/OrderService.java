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

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    private final OrderMapper orderMapper;

    private final OrderProducer orderProducer;

    @Transactional
    public OrderCreatedEvent createOrder(CreateOrderRequest request) {
        Order order = orderMapper.toEntity(request);

        orderRepository.save(order);
        log.info("Order saved with id: {}", order.getId());
        log.debug("Saved order: {}", order);

        OrderCreatedEvent event = orderMapper.toDto(order);
        orderProducer.publishOrderCreated(event);

        return event;
    }
}
