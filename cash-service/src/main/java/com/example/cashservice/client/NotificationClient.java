package com.example.cashservice.client;

import com.example.cashservice.service.NotificationOutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class NotificationClient {

    private final NotificationOutboxService notificationOutboxService;

    public void notify(Long accountId, String eventType, BigDecimal amount, String message) {
        notificationOutboxService.enqueue(accountId, eventType, amount, message);
    }
}
