package com.github.seecret1.notificationservice.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.PendingMessagesSummary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RetryScheduler {

    @Value("${app.redis.stream.consumer-group}")
    private String CONSUMER_GROUP;

    @Value("${app.redis.stream.max-batch-size}")
    private long MAX_BATCH_SIZE;

    @Value("${app.redis.stream.key}")
    private String streamKey;

    private final StringRedisTemplate redisTemplate;

    private final NotificationStreamListener notificationStreamListener;


    @Scheduled(fixedDelayString = "${app.redis.stream.retryScheduler.fixedDelay}")
    public void retryPendingMessages() {
        PendingMessagesSummary pendingSummary = redisTemplate.opsForStream()
                .pending(streamKey, CONSUMER_GROUP);

        if (pendingSummary == null || pendingSummary.getTotalPendingMessages() == 0) {
            return;
        }

        long size = Math.min(pendingSummary.getTotalPendingMessages(), MAX_BATCH_SIZE);
        var pendingMessages = redisTemplate.opsForStream()
                .pending(streamKey, CONSUMER_GROUP, Range.unbounded(), size);

        for (PendingMessage pendingMessage : pendingMessages) {
            processIfRetryIsDue(pendingMessage);
        }
    }

    private void processIfRetryIsDue(PendingMessage pendingMessage) {
        try {
            long elapsedMillis = pendingMessage.getElapsedTimeSinceLastDelivery().toMillis();
            int retryCount = resolveRetryCount(pendingMessage.getId().getValue());
            long expectedDelayMillis = calculateBackoffDelayMillis(retryCount);

            if (elapsedMillis < expectedDelayMillis) {
                return;
            }

            String messageId = pendingMessage.getId().getValue();
            List<org.springframework.data.redis.connection.stream.MapRecord<String, Object, Object>> records =
                    redisTemplate.opsForStream().range(streamKey, Range.closed(messageId, messageId));

            if (records == null || records.isEmpty()) {
                log.warn("Pending message {} was not found in stream {}", messageId, streamKey);
                return;
            }

            @SuppressWarnings("unchecked")
            org.springframework.data.redis.connection.stream.MapRecord<String, String, String> record =
                    (org.springframework.data.redis.connection.stream.MapRecord<String, String, String>)
                            (org.springframework.data.redis.connection.stream.MapRecord<?, ?, ?>) records.get(0);

            notificationStreamListener.onMessage(record);
            log.info("Retried pending message {} (attempt {})", messageId, retryCount);
        } catch (Exception e) {
            log.error("Failed to retry pending message {}", pendingMessage.getId().getValue(), e);
        }
    }

    private int resolveRetryCount(String messageId) {
        String retryCount = redisTemplate.opsForValue().get("retry:" + messageId);
        if (retryCount == null) {
            return 1;
        }

        try {
            return Integer.parseInt(retryCount);
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    private long calculateBackoffDelayMillis(int retryCount) {
        int safeRetryCount = Math.max(1, Math.min(retryCount, 20));
        return 1_000L << (safeRetryCount - 1);
    }
}