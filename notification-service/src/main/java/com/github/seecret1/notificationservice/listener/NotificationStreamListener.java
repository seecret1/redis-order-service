package com.github.seecret1.notificationservice.listener;

import com.github.seecret1.commondto.order.OrderCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class NotificationStreamListener
        implements StreamListener<String, MapRecord<String, String, String>> {

    @Override
    public void onMessage(MapRecord<String, String, String> message) {

        Map<String, String> body = message.getValue();

        try {
            OrderCreatedEvent order = new OrderCreatedEvent(
                    UUID.fromString(body.get("orderId")),
                    body.get("userId"),
                    body.get("productCode"),
                    Integer.parseInt(body.get("quantity")),
                    new BigDecimal(body.get("totalPrice")),
                    Instant.parse(body.get("timestamp"))
            );

            log.info("Order received: orderId={}, userId={}, productCode={}, quantity={}",
                    order.orderId(), order.userId(), order.productCode(), order.quantity());

            if (order.quantity() > 100) {
                throw new RuntimeException("Quantity exceeds limit: " + order.quantity());
            }

            log.info("Notification sent to user {} about order {}",
                    order.userId(), order.orderId());
        } catch (Exception ex) {
            log.error("Failed to process order: {}", ex.getMessage());
            throw new RuntimeException("Processing failed", ex);
        }
    }
}
