package com.github.seecret1.orderservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.seecret1.commondto.order.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderProducer {

    @Value("${app.redis.stream.key}")
    private String STREAM_KEY;

    private final ObjectMapper objectMapper;

    private final StringRedisTemplate stringRedisTemplate;

    public void publishOrderCreated(OrderCreatedEvent order) {
        try {
            String orderJson = objectMapper.writeValueAsString(order);

            Map<String, String> eventMap = new HashMap<>();
            eventMap.put("order", orderJson);

            var recordId = stringRedisTemplate.opsForStream().add(STREAM_KEY, eventMap);

            log.info("Event published to stream '{}' with id: {} for order: {}",
                    STREAM_KEY, recordId, order.orderId());

        } catch (Exception e) {
            log.error("Failed to publish event for order: {}", order.orderId(), e);
            throw new RuntimeException("Failed to publish order event", e);
        }
    }
}