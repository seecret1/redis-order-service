package com.github.seecret1.notificationservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;

import java.time.Duration;

@Configuration
public class StreamConfig {

    @Value("${app.redis.stream.pollTimeout}")
    private Duration pollTimeout;

    @Bean
    public StreamMessageListenerContainer<String, MapRecord<String, String, String>> streamMessageListenerContainer(
            RedisConnectionFactory redisConnectionFactory
    ) {
        var options = StreamMessageListenerContainer.
                StreamMessageListenerContainerOptions
                .builder()
                .pollTimeout(pollTimeout)
                .build();

        return StreamMessageListenerContainer.create(redisConnectionFactory, options);
    }
}
