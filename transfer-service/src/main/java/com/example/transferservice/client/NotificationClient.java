package com.example.transferservice.client;

import com.example.transferservice.dto.NotificationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class NotificationClient {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.kafka.notifications-topic}")
    private String notificationsTopic;

    public void notify(Long accountId, String eventType, BigDecimal amount, String message) {
        NotificationRequest request = new NotificationRequest(accountId, eventType, amount, message);
        try {
            kafkaTemplate.send(notificationsTopic, accountId.toString(), objectMapper.writeValueAsString(request)).get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Kafka notification publishing was interrupted", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("Kafka notification publishing failed", exception);
        }
    }
}
