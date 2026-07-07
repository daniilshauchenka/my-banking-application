package com.example.cashservice.dto;

import java.math.BigDecimal;

public record NotificationRequest(
        String login,
        String eventType,
        BigDecimal amount,
        String message
) {
}
