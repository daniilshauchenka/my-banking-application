package com.example.cashservice.service;

import com.example.cashservice.model.NotificationOutboxEvent;
import com.example.cashservice.repository.NotificationOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationOutboxPublisher {

    private final NotificationOutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelayString = "${app.outbox.scheduler-delay:5000}")
    @Transactional
    public void publishPending() {
        outboxRepository.findTop50BySentAtIsNullOrderByCreatedAtAsc()
                .forEach(this::publish);
    }

    private void publish(NotificationOutboxEvent event) {
        try {
            kafkaTemplate.send(event.getTopic(), event.getEventKey(), event.getPayload()).get();
            event.setSentAt(OffsetDateTime.now());
            event.setLastError(null);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Kafka outbox publishing was interrupted", exception);
        } catch (Exception exception) {
            event.setAttempts(event.getAttempts() + 1);
            event.setLastError(exception.getMessage());
            log.warn("Kafka outbox publishing failed for eventId={}", event.getId(), exception);
        }
    }
}
