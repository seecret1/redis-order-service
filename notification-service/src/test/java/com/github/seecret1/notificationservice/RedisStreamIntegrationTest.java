package com.github.seecret1.notificationservice;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class RedisStreamIntegrationTest {

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:8.2.1")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));

        registry.add("app.redis.stream.key", () -> "orders");
        registry.add("app.redis.stream.consumer-group", () -> "notification-group");
        registry.add("app.redis.stream.consumer-name", () -> "notification-it");
        registry.add("app.redis.stream.countRetry", () -> "1");
        registry.add("app.redis.stream.value-limited", () -> "100");
        registry.add("app.redis.stream.pollTimeout", () -> "100ms");
        registry.add("app.redis.stream.max-batch-size", () -> "50");
        registry.add("app.redis.stream.retrying", () -> "10m");
        registry.add("app.redis.stream.retryScheduler.fixedDelay", () -> "100");
    }

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void shouldRetryFailedMessageAndMoveItToDlq() {
        Map<String, String> payload = Map.of(
                "orderId", UUID.randomUUID().toString(),
                "userId", "integration-user",
                "productCode", "SKU-REDIS",
                "quantity", "999",
                "totalPrice", BigDecimal.TEN.toString(),
                "timestamp", Instant.now().toString()
        );

        RecordId recordId = redisTemplate.opsForStream().add("orders", payload);
        assertThat(recordId).isNotNull();

        Awaitility.await()
                .atMost(Duration.ofSeconds(15))
                .untilAsserted(() -> {
                    List<MapRecord<String, Object, Object>> dlqRecords = redisTemplate.opsForStream()
                            .range("orders-dlq", Range.unbounded());

                    assertThat(dlqRecords).isNotNull();
                    assertThat(dlqRecords).isNotEmpty();
                    Object originalMessageId = dlqRecords.get(0).getValue().get("originalMessageId");
                    assertThat(originalMessageId).isEqualTo(recordId.getValue());
                });
    }
}
