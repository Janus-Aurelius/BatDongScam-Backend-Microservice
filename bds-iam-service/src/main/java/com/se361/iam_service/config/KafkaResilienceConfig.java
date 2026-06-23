package com.se361.iam_service.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

@Configuration
@Slf4j
public class KafkaResilienceConfig {

    @Bean
    public CommonErrorHandler kafkaErrorHandler(KafkaTemplate<Object, Object> template) {
        // Publish to DLT after maximum retries are reached
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(template,
                (record, ex) -> {
                    log.error("[DLQ] Event failed processing permanently in IAM service. Routing to DLT topic: {}-dlt. Error: {}", 
                            record.topic(), ex.getMessage());
                    return new org.apache.kafka.common.TopicPartition(record.topic() + "-dlt", record.partition());
                });

        // Set up Exponential Backoff for retries: 1s initial delay, multiplier 2.0, max 3 attempts
        ExponentialBackOff backOff = new ExponentialBackOff(1000L, 2.0);
        backOff.setMaxAttempts(3);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);
        // Exclude poison pills (non-transient errors like bad formatting) from retries
        errorHandler.addNotRetryableExceptions(
                com.fasterxml.jackson.core.JsonProcessingException.class,
                IllegalArgumentException.class
        );
        
        return errorHandler;
    }
}
