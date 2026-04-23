package com.github.seecret1.notificationservice.config;

import com.github.seecret1.notificationservice.listener.NotificationStreamListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.Subscription;

import java.util.Map;

@Slf4j
@Configuration
public class StreamRegistration {

    @Value("${app.redis.stream.key}")
    private String STREAM_KEY;

    @Value("${app.redis.stream.consumer-group}")
    private String CONSUMER_GROUP;

    @Value("${app.redis.stream.consumer-name}")
    private String CONSUMER_NAME;

    @Bean
    public Subscription subscribe(
            StreamMessageListenerContainer<String, MapRecord<String, String, String>> container,
            NotificationStreamListener listener,
            StringRedisTemplate template
    ) {

        try {
            // XGROUP requires that the stream key exists.
            template.opsForStream().add(STREAM_KEY, Map.of("_init", "true"));
            template.opsForStream().createGroup(STREAM_KEY, CONSUMER_GROUP);
            log.info("Consumer group '{}' created for stream '{}'", CONSUMER_GROUP, STREAM_KEY);
        } catch (Exception e) {
            log.info("Consumer group '{}' already exists: {}", CONSUMER_GROUP, e.getMessage());
        }

        Subscription sub = container.receive(
                Consumer.from(CONSUMER_GROUP, CONSUMER_NAME),
                StreamOffset.create(STREAM_KEY, ReadOffset.lastConsumed()),
                listener
        );

        container.start();
        log.info("Notification consumer started for stream: {}", STREAM_KEY);

        return sub;
    }
}
