package com.github.seecret1.orderservice.repository;

import com.github.seecret1.orderservice.entity.OrderCreatedEvent;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface OrderRepository extends CrudRepository<OrderCreatedEvent, UUID> {
}
