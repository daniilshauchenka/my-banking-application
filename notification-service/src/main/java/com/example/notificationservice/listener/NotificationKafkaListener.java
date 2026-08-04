package com.example.notificationservice.listener;

import com.example.notificationservice.dto.NotificationRequest;
import com.example.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationKafkaListener {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${app.kafka.notifications-topic}")
    public void listen(String body) throws tools.jackson.core.JacksonException {
        NotificationRequest request = objectMapper.readValue(body, NotificationRequest.class);
        notificationService.notify(request);
        log.info("Notification event processed: accountId={} eventType={}", request.accountId(), request.eventType());
    }
}
