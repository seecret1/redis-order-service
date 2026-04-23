package com.github.seecret1.notificationservice.listener;

import com.github.seecret1.commondto.order.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationStreamListener
        implements StreamListener<String, MapRecord<String, String, String>> {

    @Value("${app.redis.stream.key}")
    private String STREAM_KEY;

    @Value("${app.redis.stream.consumer-group}")
    private String CONSUMER_GROUP;

    @Value("${app.redis.stream.countRetry}")
    private int countRetry;

    @Value("${app.redis.stream.retrying}")
    private Duration retryingDuration;

    @Value("${app.redis.stream.value-limited}")
    private int valueLimited;

    private final StringRedisTemplate redisTemplate;

    @Override
    public void onMessage(MapRecord<String, String, String> message) {

        Map<String, String> body = message.getValue();

        try {
            OrderCreatedEvent order = parseEvent(body);

            log.info("Order received: orderId={}, userId={}, productCode={}, quantity={}",
                    order.orderId(), order.userId(), order.productCode(), order.quantity());

            if (order.quantity() > valueLimited) {
                throw new RuntimeException("Quantity exceeds limit: " + order.quantity());
            }

            redisTemplate.opsForStream().acknowledge(STREAM_KEY, CONSUMER_GROUP, message.getId());
            log.info("Notification sent to user {} about order {}",
                    order.userId(), order.orderId());

        } catch (Exception ex) {
            log.error("Failed to process order: {}", ex.getMessage());
            handleFailure(message, ex);
        }
    }

    private void handleFailure(MapRecord<String, String, String> message, Exception error) {
        String messageId = message.getId().getValue();
        String retryKey = "retry:" + messageId;

        String retryCountStr = redisTemplate.opsForValue().get(retryKey);
        int retryCount = retryCountStr == null ? 0 : Integer.parseInt(retryCountStr);

        if (retryCount < countRetry) {
            redisTemplate.opsForValue().set(retryKey, String.valueOf(retryCount + 1), retryingDuration);
            log.warn("Message {} will be retried (attempt {}/{})", messageId, retryCount + 1, countRetry);
        } else {
            log.error("Message {} failed after {} attempts, sending to DLQ", messageId, countRetry);
            sendToDeadLetterQueue(message, error);

            redisTemplate.opsForStream().acknowledge(STREAM_KEY, CONSUMER_GROUP, message.getId());
            redisTemplate.delete(retryKey);
        }
    }

    private void sendToDeadLetterQueue(MapRecord<String, String, String> message, Exception error) {
        try {
            String dlqKey = "orders-dlq";
            Map<String, String> originalData = message.getValue();

            Map<String, String> dlqMessage = Map.of(
                    "originalOrderId", originalData.get("orderId"),
                    "originalUserId", originalData.get("userId"),
                    "originalProductCode", originalData.get("productCode"),
                    "originalQuantity", originalData.get("quantity"),
                    "originalTotalPrice", originalData.get("totalPrice"),
                    "originalTimestamp", originalData.get("timestamp"),
                    "error", error.getMessage(),
                    "failedAt", Instant.now().toString(),
                    "originalMessageId", message.getId().getValue()
            );

            redisTemplate.opsForStream().add(dlqKey, dlqMessage);
            log.info("Message sent to DLQ stream: {}", dlqKey);
        } catch (Exception e) {
            log.error("Failed to send message to DLQ", e);
        }
    }

    private OrderCreatedEvent parseEvent(Map<String, String> body) {
        return new OrderCreatedEvent(
                UUID.fromString(body.get("orderId")),
                body.get("userId"),
                body.get("productCode"),
                Integer.parseInt(body.get("quantity")),
                new BigDecimal(body.get("totalPrice")),
                Instant.parse(body.get("timestamp"))
        );
    }
}
