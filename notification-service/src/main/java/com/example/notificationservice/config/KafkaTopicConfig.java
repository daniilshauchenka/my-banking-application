package com.example.notificationservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    @ConditionalOnProperty(name = "spring.kafka.listener.auto-startup", havingValue = "true", matchIfMissing = true)
    public NewTopic notificationsTopic(
            @Value("${app.kafka.notifications-topic}") String topicName,
            @Value("${app.kafka.notifications-topic-partitions:1}") int partitions,
            @Value("${app.kafka.notifications-topic-replicas:1}") int replicas
    ) {
        return TopicBuilder.name(topicName)
                .partitions(partitions)
                .replicas(replicas)
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "spring.kafka.listener.auto-startup", havingValue = "true", matchIfMissing = true)
    public NewTopic notificationsDltTopic(
            @Value("${app.kafka.notifications-dlt-topic:bank.notifications.dlt}") String topicName,
            @Value("${app.kafka.notifications-topic-partitions:1}") int partitions,
            @Value("${app.kafka.notifications-topic-replicas:1}") int replicas
    ) {
        return TopicBuilder.name(topicName)
                .partitions(partitions)
                .replicas(replicas)
                .build();
    }
}
