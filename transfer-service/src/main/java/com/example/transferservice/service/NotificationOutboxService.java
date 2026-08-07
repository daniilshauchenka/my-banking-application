package com.example.transferservice.service;

import com.example.transferservice.dto.NotificationRequest;
import com.example.transferservice.model.NotificationOutboxEvent;
import com.example.transferservice.repository.NotificationOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class NotificationOutboxService {

    private final NotificationOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.kafka.notifications-topic}")
    private String notificationsTopic;

    @Transactional
    public void enqueue(Long accountId, String eventType, BigDecimal amount, String message) {
        NotificationRequest request = new NotificationRequest(accountId, eventType, amount, message);
        try {
            outboxRepository.save(NotificationOutboxEvent.builder()
                    .eventKey(accountId.toString())
                    .topic(notificationsTopic)
                    .payload(objectMapper.writeValueAsString(request))
                    .build());
        } catch (Exception exception) {
            throw new IllegalStateException("Notification outbox event serialization failed", exception);
        }
    }
}
