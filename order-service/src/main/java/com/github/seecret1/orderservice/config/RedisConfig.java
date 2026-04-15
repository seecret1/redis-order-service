package com.github.seecret1.orderservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.seecret1.orderservice.entity.OrderCreatedEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, OrderCreatedEvent> redisTemplate(
            RedisConnectionFactory redisConnectionFactory,
            ObjectMapper objectMapper
    ) {
        RedisTemplate<String, OrderCreatedEvent> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);

        redisTemplate.setKeySerializer(new StringRedisSerializer());

        redisTemplate.setValueSerializer(
                new Jackson2JsonRedisSerializer<>(
                        objectMapper,
                        OrderCreatedEvent.class
                )
        );

        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }
}
