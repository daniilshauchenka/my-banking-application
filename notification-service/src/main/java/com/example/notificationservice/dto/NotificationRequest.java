package com.example.notificationservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record NotificationRequest(
        @NotNull Long accountId,
        @NotBlank String eventType,
        @NotNull BigDecimal amount,
        @NotBlank String message
) {
}
