package com.example.notificationservice.listener;

import com.example.notificationservice.dto.NotificationRequest;
import com.example.notificationservice.service.NotificationService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationKafkaListener {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    @KafkaListener(topics = "${app.kafka.notifications-topic}")
    public void listen(ConsumerRecord<String, String> record) throws tools.jackson.core.JacksonException {
        NotificationRequest request = objectMapper.readValue(record.value(), NotificationRequest.class);
        Set<ConstraintViolation<NotificationRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }

        notificationService.notify(request);
        log.info(
                "Notification event processed: key={} topic={} partition={} offset={} accountId={} eventType={}",
                record.key(),
                record.topic(),
                record.partition(),
                record.offset(),
                request.accountId(),
                request.eventType()
        );
    }
}
