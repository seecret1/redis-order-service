package com.github.seecret1.orderservice.repository;

import com.github.seecret1.orderservice.api.Order;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface OrderRepository extends CrudRepository<Order, UUID> {
}
