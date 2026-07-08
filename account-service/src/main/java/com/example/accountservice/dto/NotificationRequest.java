package com.example.accountservice.dto;

import java.math.BigDecimal;

public record NotificationRequest(
        Long accountId,
        String eventType,
        BigDecimal amount,
        String message
) {
}
